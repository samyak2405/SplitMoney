package com.splitmoney.application.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        String hyperswitchRefundId,
        OffsetDateTime createdAt
) {}
