package com.splitmoney.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.hyperswitch")
public record HyperswitchProperties(
        String baseUrl,
        String apiKey,
        String publishableKey,
        String webhookSecret,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxRetries
) {
}
