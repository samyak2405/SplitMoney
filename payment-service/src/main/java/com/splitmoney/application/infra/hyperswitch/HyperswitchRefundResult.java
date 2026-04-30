package com.splitmoney.application.infra.hyperswitch;

import java.math.BigDecimal;

public record HyperswitchRefundResult(
        String hyperswitchRefundId,
        String status,   // "succeeded" | "failed" | "pending"
        BigDecimal amount
) {}
