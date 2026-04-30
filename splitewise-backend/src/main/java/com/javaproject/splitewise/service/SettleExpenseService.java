package com.javaproject.splitewise.service;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.ExpenseSettlementRequest;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.SettleExpenseResponse;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.exception.custom.ProcessApiException;
import com.javaproject.splitewise.model.GroupBalance;
import com.javaproject.splitewise.model.SagaTransaction;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.GroupBalanceRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.SagaRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.splitmoney.grpc.payment.InitiatePaymentRequest;
import com.splitmoney.grpc.payment.InitiatePaymentResponse;
import com.splitmoney.grpc.payment.PaymentGrpcServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettleExpenseService implements Processor {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBalanceRepository groupBalanceRepository;
    private final SagaRepository sagaRepository;
    private final PaymentGrpcServiceGrpc.PaymentGrpcServiceBlockingStub paymentGrpcStub;

    @Override
    @Transactional
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        ExpenseSettlementRequest request = (ExpenseSettlementRequest) apiRequest;

        String payerEmail = request.getPaidByEmail().trim().toLowerCase(Locale.ROOT);
        String payeeEmail = request.getPaidTo().trim().toLowerCase(Locale.ROOT);
        Long groupId = request.getGroupId();
        BigDecimal settlementAmount = parseAmount(request.getAmount());
        String currency = request.getCurrency().trim().toUpperCase(Locale.ROOT);
        String paymentMethod = request.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        String returnUrl = request.getReturnUrl() != null ? request.getReturnUrl().trim() : "";

        log.info("settle.initiate groupId={} payer={} payee={} amount={} method={}",
                groupId, payerEmail, payeeEmail, settlementAmount, paymentMethod);

        // ── 1. Look up users ──────────────────────────────────────────────────
        User payer = userRepository.findByEmail(payerEmail)
                .orElseThrow(() -> new ApiValidationException("PAYER_NOT_FOUND", HttpStatus.NOT_FOUND));
        User payee = userRepository.findByEmail(payeeEmail)
                .orElseThrow(() -> new ApiValidationException("PAYEE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (payer.getId().equals(payee.getId())) {
            throw new ApiValidationException("SELF_SETTLEMENT_NOT_ALLOWED", HttpStatus.BAD_REQUEST);
        }

        // ── 2. Validate group membership ─────────────────────────────────────
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, payer.getId()) ||
            !groupMemberRepository.existsByGroupIdAndUserId(groupId, payee.getId())) {
            throw new ApiValidationException("USER_NOT_IN_GROUP", HttpStatus.FORBIDDEN);
        }

        // ── 3. Validate balance (no writes — balance adjusts after payment) ──
        GroupBalance payerBalance = groupBalanceRepository
                .findByGroupIdAndUserId(groupId, payer.getId())
                .orElse(zeroBalance(groupId, payer.getId()));

        BigDecimal payerNet = parseAmount(payerBalance.getNetBalance());
        if (payerNet.compareTo(BigDecimal.ZERO) >= 0) {
            throw new ApiValidationException("PAYER_HAS_NO_DEBT_IN_GROUP", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        GroupBalance payeeBalance = groupBalanceRepository
                .findByGroupIdAndUserId(groupId, payee.getId())
                .orElse(zeroBalance(groupId, payee.getId()));

        BigDecimal payeeNet = parseAmount(payeeBalance.getNetBalance());
        if (payeeNet.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiValidationException("PAYEE_HAS_NO_CREDIT_IN_GROUP", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        BigDecimal maxSettleable = payerNet.abs().min(payeeNet);
        if (settlementAmount.compareTo(maxSettleable) > 0) {
            throw new ApiValidationException(
                    "SETTLEMENT_EXCEEDS_BALANCE. Max settleable: " + format(maxSettleable),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // ── 4. Create saga record ─────────────────────────────────────────────
        UUID sagaId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        SagaTransaction saga = new SagaTransaction();
        saga.setId(sagaId);
        saga.setGroupId(groupId);
        saga.setPayerUserId(payer.getId());
        saga.setPayeeUserId(payee.getId());
        saga.setAmount(settlementAmount);
        saga.setCurrency(currency);
        saga.setPaymentMethod(paymentMethod);
        saga.setStatus("VALIDATION_PASSED");
        saga.setCreatedAt(now);
        saga.setUpdatedAt(now);
        sagaRepository.save(saga);

        // ── 5. Initiate payment via gRPC ─────────────────────────────────────
        InitiatePaymentRequest grpcReq = InitiatePaymentRequest.newBuilder()
                .setPayerUserId(payer.getId().toString())
                .setPayeeUserId(payee.getId().toString())
                .setAmount(format(settlementAmount))
                .setCurrency(currency)
                .setPaymentMethod(paymentMethod)
                .setClientRequestId(sagaId.toString())
                .setReturnUrl(returnUrl)
                .build();

        InitiatePaymentResponse grpcResp;
        try {
            grpcResp = paymentGrpcStub.initiatePayment(grpcReq);
        } catch (Exception ex) {
            saga.setStatus("PAYMENT_FAILED");
            saga.setErrorReason("gRPC call failed: " + ex.getMessage());
            saga.setUpdatedAt(OffsetDateTime.now());
            sagaRepository.save(saga);
            log.error("settle.grpc-failed sagaId={} reason={}", sagaId, ex.getMessage());
            throw new ProcessApiException("Payment service unavailable", HttpStatus.BAD_GATEWAY);
        }

        if (!grpcResp.getSuccess()) {
            saga.setStatus("PAYMENT_FAILED");
            saga.setErrorReason(grpcResp.getErrorMessage());
            saga.setUpdatedAt(OffsetDateTime.now());
            sagaRepository.save(saga);
            log.warn("settle.payment-rejected sagaId={} reason={}", sagaId, grpcResp.getErrorMessage());
            throw new ProcessApiException(grpcResp.getErrorMessage(), HttpStatus.BAD_REQUEST);
        }

        // ── 6. Update saga with payment details ───────────────────────────────
        UUID paymentId = UUID.fromString(grpcResp.getPaymentId());
        saga.setPaymentId(paymentId);
        saga.setCheckoutUrl(grpcResp.getCheckoutUrl().isBlank() ? null : grpcResp.getCheckoutUrl());
        saga.setStatus("PAYMENT_INITIATED");
        saga.setUpdatedAt(OffsetDateTime.now());
        sagaRepository.save(saga);

        log.info("settle.initiated sagaId={} paymentId={} checkoutUrl={}",
                sagaId, paymentId, saga.getCheckoutUrl());

        SettleExpenseResponse data = SettleExpenseResponse.builder()
                .sagaId(sagaId)
                .paymentId(paymentId)
                .checkoutUrl(saga.getCheckoutUrl())
                .status("PAYMENT_INITIATED")
                .groupId(groupId)
                .paidByEmail(payer.getEmail())
                .paidToEmail(payee.getEmail())
                .amount(format(settlementAmount))
                .currency(currency)
                .paymentMethod(paymentMethod)
                .build();

        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Payment initiated successfully")
                .timestamp(OffsetDateTime.now())
                .data(data)
                .build();
    }

    private GroupBalance zeroBalance(Long groupId, UUID userId) {
        return GroupBalance.builder()
                .groupId(groupId)
                .userId(userId)
                .netBalance("0.00")
                .updatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new ProcessApiException("INVALID_AMOUNT_FORMAT", HttpStatus.BAD_REQUEST);
        }
    }

    private String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
