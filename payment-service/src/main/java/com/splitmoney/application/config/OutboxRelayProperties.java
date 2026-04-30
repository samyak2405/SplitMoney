package com.splitmoney.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox.relay")
public record OutboxRelayProperties(long fixedDelayMs, int batchSize) {
}
