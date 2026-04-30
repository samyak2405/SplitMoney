package com.splitmoney.application.api.dto;

import java.util.UUID;

public record PaymentReturnResponse(
        UUID paymentId,
        String status,
        String message
) {}
