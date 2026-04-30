package com.javaproject.splitewise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SettlementCompleteRequest(
        @NotNull UUID paymentId,
        @NotNull UUID payerUserId,
        @NotNull UUID payeeUserId,
        @NotBlank String amount,
        @NotBlank String currency
) {}
