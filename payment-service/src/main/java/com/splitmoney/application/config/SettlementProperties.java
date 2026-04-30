package com.splitmoney.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.settlement")
public record SettlementProperties(
        String splitwiseBaseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
    public SettlementProperties {
        if (splitwiseBaseUrl == null || splitwiseBaseUrl.isBlank()) splitwiseBaseUrl = "http://localhost:8081";
        if (connectTimeoutMs <= 0) connectTimeoutMs = 3000;
        if (readTimeoutMs <= 0) readTimeoutMs = 5000;
    }
}
