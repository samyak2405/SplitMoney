package com.splitmoney.application.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WebhookEventRequest(
        @JsonProperty("webhook_id") String webhookId,
        @JsonProperty("payment_id") String paymentId,
        @JsonProperty("status") String status
) {
}
