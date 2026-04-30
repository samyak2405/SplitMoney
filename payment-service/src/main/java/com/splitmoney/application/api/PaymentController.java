package com.splitmoney.application.api;

import com.splitmoney.application.api.dto.CreatePaymentRequest;
import com.splitmoney.application.api.dto.PaymentListResponse;
import com.splitmoney.application.api.dto.PaymentResponse;
import com.splitmoney.application.api.dto.PaymentReturnResponse;
import com.splitmoney.application.api.dto.RefundRequest;
import com.splitmoney.application.api.dto.RefundResponse;
import com.splitmoney.application.domain.payment.PaymentService;
import com.splitmoney.application.domain.refund.RefundService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final RefundService refundService;

    public PaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    // ── Payment CRUD ─────────────────────────────────────────────────────────

    @PostMapping("/v1/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        LOGGER.info("create payment payerUserId={} payeeUserId={} amount={} currency={} method={}",
                request.payerUserId(), request.payeeUserId(), request.amount(), request.currency(), request.paymentMethod());
        return paymentService.createPayment(request, idempotencyKey);
    }

    @GetMapping("/v1/payments/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/v1/payments")
    public PaymentListResponse list(
            @RequestParam UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String after
    ) {
        limit = Math.max(1, Math.min(limit, 100));
        return paymentService.listPayments(userId, status, limit, after);
    }

    @PostMapping("/v1/payments/{id}/cancel")
    public PaymentResponse cancel(@PathVariable UUID id) {
        LOGGER.info("cancel payment paymentId={}", id);
        return paymentService.cancelPayment(id);
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @PostMapping("/v1/payments/{id}/refund")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse refund(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request
    ) {
        LOGGER.info("refund payment paymentId={} amount={} reason={}", id, request.amount(), request.reason());
        return refundService.initiateRefund(id, request);
    }

    // ── Checkout return handler ───────────────────────────────────────────────
    // Hyperswitch redirects users here after checkout. Frontend polls this
    // until status is terminal (COMPLETED / FAILED / CANCELLED).

    @GetMapping("/payment/return")
    public PaymentReturnResponse returnHandler(@RequestParam UUID paymentId) {
        return paymentService.getReturnStatus(paymentId);
    }
}
