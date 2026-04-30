package com.splitmoney.application.grpc;

import com.splitmoney.application.api.dto.CreatePaymentRequest;
import com.splitmoney.application.api.dto.PaymentResponse;
import com.splitmoney.application.domain.payment.PaymentMethodType;
import com.splitmoney.application.domain.payment.PaymentService;
import com.splitmoney.grpc.payment.GetPaymentStatusRequest;
import com.splitmoney.grpc.payment.GetPaymentStatusResponse;
import com.splitmoney.grpc.payment.InitiatePaymentRequest;
import com.splitmoney.grpc.payment.InitiatePaymentResponse;
import com.splitmoney.grpc.payment.CancelPaymentRequest;
import com.splitmoney.grpc.payment.CancelPaymentResponse;
import com.splitmoney.grpc.payment.PaymentGrpcServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGrpcServiceImpl extends PaymentGrpcServiceGrpc.PaymentGrpcServiceImplBase {

    private final PaymentService paymentService;

    @Override
    public void initiatePayment(InitiatePaymentRequest request,
                                StreamObserver<InitiatePaymentResponse> responseObserver) {
        log.info("grpc.initiatePayment sagaId={} payerUserId={} amount={}",
                request.getClientRequestId(), request.getPayerUserId(), request.getAmount());
        try {
            CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                    UUID.fromString(request.getPayerUserId()),
                    UUID.fromString(request.getPayeeUserId()),
                    new BigDecimal(request.getAmount()),
                    request.getCurrency(),
                    PaymentMethodType.valueOf(request.getPaymentMethod()),
                    request.getClientRequestId(),
                    request.getReturnUrl().isBlank() ? null : request.getReturnUrl()
            );

            PaymentResponse response = paymentService.createPayment(
                    paymentRequest, request.getClientRequestId());

            InitiatePaymentResponse grpcResponse = InitiatePaymentResponse.newBuilder()
                    .setPaymentId(response.paymentId().toString())
                    .setCheckoutUrl(response.checkoutUrl() != null ? response.checkoutUrl() : "")
                    .setStatus(response.status().name())
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
            log.info("grpc.initiatePayment success paymentId={} status={}",
                    response.paymentId(), response.status());
        } catch (Exception ex) {
            log.error("grpc.initiatePayment failed sagaId={} reason={}", request.getClientRequestId(), ex.getMessage());
            responseObserver.onNext(InitiatePaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(ex.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getPaymentStatus(GetPaymentStatusRequest request,
                                 StreamObserver<GetPaymentStatusResponse> responseObserver) {
        log.info("grpc.getPaymentStatus paymentId={}", request.getPaymentId());
        try {
            PaymentResponse response = paymentService.getPayment(UUID.fromString(request.getPaymentId()));
            responseObserver.onNext(GetPaymentStatusResponse.newBuilder()
                    .setPaymentId(response.paymentId().toString())
                    .setStatus(response.status().name())
                    .setMessage(response.status().name())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("grpc.getPaymentStatus failed paymentId={} reason={}", request.getPaymentId(), ex.getMessage());
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void cancelPayment(CancelPaymentRequest request,
                              StreamObserver<CancelPaymentResponse> responseObserver) {
        log.info("grpc.cancelPayment paymentId={}", request.getPaymentId());
        try {
            paymentService.cancelPayment(UUID.fromString(request.getPaymentId()));
            responseObserver.onNext(CancelPaymentResponse.newBuilder()
                    .setSuccess(true).setMessage("Payment cancelled").build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onNext(CancelPaymentResponse.newBuilder()
                    .setSuccess(false).setMessage(ex.getMessage()).build());
            responseObserver.onCompleted();
        }
    }
}
