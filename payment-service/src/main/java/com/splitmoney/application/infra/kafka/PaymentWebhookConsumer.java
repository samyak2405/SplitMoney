package com.splitmoney.application.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.application.domain.payment.PaymentProcessingService;
import com.splitmoney.application.infra.kafka.events.PaymentWebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentWebhookConsumer.class);
    private final ObjectMapper objectMapper;
    private final PaymentProcessingService paymentProcessingService;

    public PaymentWebhookConsumer(ObjectMapper objectMapper, PaymentProcessingService paymentProcessingService) {
        this.objectMapper = objectMapper;
        this.paymentProcessingService = paymentProcessingService;
    }

    @KafkaListener(topics = "payment.webhook_received", groupId = "payment-webhooks")
    public void onWebhook(String payload) throws Exception {
        PaymentWebhookEvent event = objectMapper.readValue(payload, PaymentWebhookEvent.class);
        LOGGER.info("kafka webhook event received webhookId={} hyperswitchPaymentId={} status={}",
                event.webhookId(), event.hyperswitchPaymentId(), event.status());
        paymentProcessingService.handleWebhook(event);
    }
}
