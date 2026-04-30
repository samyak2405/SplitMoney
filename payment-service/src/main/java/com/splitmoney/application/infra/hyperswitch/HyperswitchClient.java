package com.splitmoney.application.infra.hyperswitch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.application.config.HyperswitchProperties;
import com.splitmoney.application.domain.patterns.port.PaymentGatewayPort;
import com.splitmoney.application.domain.patterns.strategy.PaymentMethodStrategyResolver;
import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class HyperswitchClient implements PaymentGatewayPort {

    private final RestClient restClient;
    private final HyperswitchProperties properties;
    private final PaymentMethodStrategyResolver strategyResolver;
    private final ObjectMapper objectMapper;

    private int consecutiveFailures = 0;
    private Instant circuitOpenedUntil = Instant.EPOCH;

    public HyperswitchClient(
            RestClient.Builder restClientBuilder,
            HyperswitchProperties properties,
            PaymentMethodStrategyResolver strategyResolver,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
        this.properties = properties;
        this.strategyResolver = strategyResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized HyperswitchPaymentResult createPayment(Payment payment) {
        ensureCircuitClosed();
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= Math.max(1, properties.maxRetries()); attempt++) {
            try {
                log.info("calling hyperswitch create payment paymentId={} attempt={} method={}",
                        payment.getId(), attempt, payment.getPaymentMethod());
                Map<String, Object> payload = new HashMap<>(strategyResolver
                        .resolve(payment.getPaymentMethod())
                        .buildGatewayPayload(payment));
                payload.put("merchant_order_reference_id", payment.getId().toString());
                if (payment.getReturnUrl() != null && !payment.getReturnUrl().isBlank()) {
                    payload.put("return_url", payment.getReturnUrl());
                }

                String response = restClient.post()
                        .uri("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("api-key", properties.apiKey())
                        .body(payload)
                        .retrieve()
                        .body(String.class);
                resetCircuit();
                HyperswitchPaymentResult result = toResult(response, payment.getId().toString());
                log.info("hyperswitch create payment success paymentId={} hyperswitchPaymentId={} status={} checkoutUrlPresent={}",
                        payment.getId(), result.hyperswitchPaymentId(), result.status(), result.checkoutUrl() != null);
                return result;
            } catch (RuntimeException ex) {
                lastError = ex;
                markFailure();
                log.warn("hyperswitch create payment failed paymentId={} attempt={} reason={}",
                        payment.getId(), attempt, ex.getMessage());
                try {
                    Thread.sleep((long) (200L * attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw lastError != null ? lastError : new IllegalStateException("Hyperswitch call failed");
    }

    @Override
    public synchronized HyperswitchPaymentResult fetchPayment(String hyperswitchPaymentId) {
        ensureCircuitClosed();
        try {
            log.info("calling hyperswitch fetch payment hyperswitchPaymentId={}", hyperswitchPaymentId);
            String response = restClient.get()
                    .uri("/payments/{id}", hyperswitchPaymentId)
                    .header("api-key", properties.apiKey())
                    .retrieve()
                    .body(String.class);
            resetCircuit();
            HyperswitchPaymentResult result = toResult(response, hyperswitchPaymentId);
            log.info("hyperswitch fetch payment success hyperswitchPaymentId={} status={}",
                    hyperswitchPaymentId, result.status());
            return result;
        } catch (RuntimeException ex) {
            markFailure();
            log.warn("hyperswitch fetch payment failed hyperswitchPaymentId={} reason={}",
                    hyperswitchPaymentId, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public synchronized void cancelPayment(String hyperswitchPaymentId) {
        ensureCircuitClosed();
        try {
            log.info("calling hyperswitch cancel payment hyperswitchPaymentId={}", hyperswitchPaymentId);
            restClient.post()
                    .uri("/payments/{id}/cancel", hyperswitchPaymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", properties.apiKey())
                    .body(Map.of("cancellation_reason", "requested_by_customer"))
                    .retrieve()
                    .toBodilessEntity();
            resetCircuit();
            log.info("hyperswitch cancel payment success hyperswitchPaymentId={}", hyperswitchPaymentId);
        } catch (RuntimeException ex) {
            markFailure();
            log.warn("hyperswitch cancel payment failed hyperswitchPaymentId={} reason={}", hyperswitchPaymentId, ex.getMessage());
            throw ex;
        }
    }

    @Override
    public synchronized HyperswitchRefundResult refundPayment(String hyperswitchPaymentId, BigDecimal amount, String reason) {
        ensureCircuitClosed();
        try {
            log.info("calling hyperswitch refund payment hyperswitchPaymentId={} amount={}", hyperswitchPaymentId, amount);
            Map<String, Object> payload = new HashMap<>();
            payload.put("payment_id", hyperswitchPaymentId);
            payload.put("amount", amount);
            payload.put("reason", mapRefundReason(reason));

            String response = restClient.post()
                    .uri("/refunds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("api-key", properties.apiKey())
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            resetCircuit();
            HyperswitchRefundResult result = toRefundResult(response);
            log.info("hyperswitch refund success hyperswitchRefundId={} status={}", result.hyperswitchRefundId(), result.status());
            return result;
        } catch (RuntimeException ex) {
            markFailure();
            log.warn("hyperswitch refund failed hyperswitchPaymentId={} reason={}", hyperswitchPaymentId, ex.getMessage());
            throw ex;
        }
    }

    private HyperswitchRefundResult toRefundResult(String response) {
        try {
            JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
            String refundId = node.path("refund_id").asText(null);
            String status   = node.path("status").asText("pending");
            BigDecimal amount = node.path("amount").decimalValue();
            return new HyperswitchRefundResult(refundId, status, amount);
        } catch (Exception ex) {
            return new HyperswitchRefundResult(null, "pending", BigDecimal.ZERO);
        }
    }

    private String mapRefundReason(String reason) {
        if (reason == null) return "customer";
        return switch (reason.toUpperCase()) {
            case "DUPLICATE"             -> "duplicate";
            case "FRAUDULENT"            -> "fraudulent";
            case "REQUESTED_BY_CUSTOMER" -> "customer";
            default                      -> "customer";
        };
    }

    private HyperswitchPaymentResult toResult(String response, String fallbackPaymentId) {
        try {
            log.debug("hyperswitch raw response: {}", response);
            JsonNode node = objectMapper.readTree(response == null ? "{}" : response);
            String gatewayId = node.path("payment_id").asText(fallbackPaymentId);
            String statusRaw = node.path("status").asText("processing").toUpperCase();
            PaymentStatus mapped = switch (statusRaw) {
                case "SUCCEEDED", "COMPLETED", "CHARGED" -> PaymentStatus.COMPLETED;
                case "FAILED", "CANCELLED", "VOIDED"     -> PaymentStatus.FAILED;
                case "PENDING", "REQUIRES_CUSTOMER_ACTION", "REQUIRES_PAYMENT_METHOD" -> PaymentStatus.PENDING;
                default -> PaymentStatus.PROCESSING;
            };

            // 1. Try direct redirect URLs from next_action (e.g. 3DS, bank redirect)
            String checkoutUrl = firstNonBlank(
                    node.at("/next_action/redirect_to_url").asText(null),
                    node.at("/next_action/url").asText(null),
                    node.at("/redirection_data/url").asText(null),
                    node.at("/payment_link/link").asText(null)
            );

            // 2. Fall back to building Hyperswitch hosted checkout from client_secret
            if (checkoutUrl == null) {
                String clientSecret = node.path("client_secret").asText(null);
                String pk = properties.publishableKey();
                if (clientSecret != null && !clientSecret.isBlank()
                        && pk != null && !pk.isBlank() && !pk.startsWith("pk_snd_your")) {
                    checkoutUrl = "https://checkout.hyperswitch.io/?client_secret="
                            + clientSecret + "&publishable_key=" + pk;
                    log.info("hyperswitch built hosted checkout url for paymentId={}", fallbackPaymentId);
                }
            }

            return new HyperswitchPaymentResult(gatewayId, mapped, checkoutUrl);
        } catch (Exception ex) {
            log.error("hyperswitch parse response failed fallbackId={} reason={}", fallbackPaymentId, ex.getMessage());
            return new HyperswitchPaymentResult(fallbackPaymentId, PaymentStatus.PROCESSING, null);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void ensureCircuitClosed() {
        if (Instant.now().isBefore(circuitOpenedUntil)) {
            throw new IllegalStateException("Hyperswitch circuit is open");
        }
    }

    private void markFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= 5) {
            circuitOpenedUntil = Instant.now().plus(Duration.ofSeconds(10));
            consecutiveFailures = 0;
        }
    }

    private void resetCircuit() {
        consecutiveFailures = 0;
        circuitOpenedUntil = Instant.EPOCH;
    }
}
