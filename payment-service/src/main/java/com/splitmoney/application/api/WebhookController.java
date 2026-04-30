package com.splitmoney.application.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.application.api.dto.WebhookEventRequest;
import com.splitmoney.application.infra.kafka.events.PaymentWebhookEvent;
import com.splitmoney.application.infra.webhook.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/webhooks/hyperswitch")
public class WebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookController.class);
    private final WebhookSignatureValidator validator;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public WebhookController(
            WebhookSignatureValidator validator,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receive(
            @RequestBody String payload,
            @RequestHeader(value = "x-webhook-signature-256", required = false) String signature256,
            @RequestHeader(value = "X-Hyperswitch-Signature", required = false) String signatureLegacy
    ) {
        LOGGER.info("hyperswitch webhook received payloadSize={} hasSignature256={} hasLegacySignature={}",
                payload.length(), signature256 != null, signatureLegacy != null);
        String signature = signature256 != null ? signature256 : signatureLegacy;
        if (!validator.isValid(payload, signature)) {
            LOGGER.warn("hyperswitch webhook signature validation failed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }
        try {
            WebhookEventRequest request = objectMapper.readValue(payload, WebhookEventRequest.class);
            LOGGER.info("hyperswitch webhook accepted webhookId={} hyperswitchPaymentId={} status={}",
                    request.webhookId(), request.paymentId(), request.status());
            PaymentWebhookEvent event = new PaymentWebhookEvent(
                    request.webhookId(),
                    request.paymentId(),
                    request.status()
            );
            kafkaTemplate.send("payment.webhook_received", request.paymentId(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            LOGGER.warn("hyperswitch webhook payload parse failed reason={}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook payload");
        }
    }
}
