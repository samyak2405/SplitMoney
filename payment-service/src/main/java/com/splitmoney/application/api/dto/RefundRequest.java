package com.splitmoney.application.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record RefundRequest(
        @DecimalMin(value = "0.01", message = "refund amount must be at least 0.01")
        BigDecimal amount,

        @Pattern(regexp = "DUPLICATE|FRAUDULENT|REQUESTED_BY_CUSTOMER",
                 message = "reason must be DUPLICATE, FRAUDULENT, or REQUESTED_BY_CUSTOMER")
        String reason
) {}
