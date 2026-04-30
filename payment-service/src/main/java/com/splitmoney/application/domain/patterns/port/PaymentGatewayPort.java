package com.splitmoney.application.domain.patterns.port;

import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.infra.hyperswitch.HyperswitchPaymentResult;
import com.splitmoney.application.infra.hyperswitch.HyperswitchRefundResult;

import java.math.BigDecimal;

public interface PaymentGatewayPort {
    HyperswitchPaymentResult createPayment(Payment payment);
    HyperswitchPaymentResult fetchPayment(String hyperswitchPaymentId);
    void cancelPayment(String hyperswitchPaymentId);
    HyperswitchRefundResult refundPayment(String hyperswitchPaymentId, BigDecimal amount, String reason);
}
