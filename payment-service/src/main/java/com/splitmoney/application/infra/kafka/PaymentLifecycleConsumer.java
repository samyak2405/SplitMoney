package com.splitmoney.application.infra.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.application.domain.payment.PaymentProcessingService;
import com.splitmoney.application.infra.kafka.events.PaymentLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentLifecycleConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentLifecycleConsumer.class);
    private final ObjectMapper objectMapper;
    private final PaymentProcessingService paymentProcessingService;

    public PaymentLifecycleConsumer(ObjectMapper objectMapper, PaymentProcessingService paymentProcessingService) {
        this.objectMapper = objectMapper;
        this.paymentProcessingService = paymentProcessingService;
    }

    @KafkaListener(topics = "payment.lifecycle", groupId = "payment-processors")
    public void onPaymentLifecycle(String payload) throws Exception {
        PaymentLifecycleEvent event = objectMapper.readValue(payload, PaymentLifecycleEvent.class);
        LOGGER.info("kafka payment lifecycle event received paymentId={} status={}", event.paymentId(), event.status());
        if ("VALIDATING".equals(event.status().name())) {
            paymentProcessingService.processInitiatedPayment(event);
        }
    }
}
