package com.splitmoney.application.api.dto;

import com.splitmoney.application.domain.payment.PaymentMethodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID payerUserId,
        @NotNull UUID payeeUserId,
        @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull PaymentMethodType paymentMethod,
        @NotBlank String clientRequestId,
        @Size(max = 1024) String returnUrl
) {
}
