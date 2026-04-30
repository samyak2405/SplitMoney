package com.splitmoney.application.infra.resilience;

import com.splitmoney.application.domain.payment.PaymentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StalePaymentRecoveryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StalePaymentRecoveryScheduler.class);
    private final PaymentProcessingService paymentProcessingService;
    private final int staleSeconds;

    public StalePaymentRecoveryScheduler(
            PaymentProcessingService paymentProcessingService,
            @Value("${app.payment.stale-processing-seconds:300}") int staleSeconds
    ) {
        this.paymentProcessingService = paymentProcessingService;
        this.staleSeconds = staleSeconds;
    }

    @Scheduled(fixedDelayString = "${app.payment.recovery-fixed-delay-ms:60000}")
    public void reconcileStuckPayments() {
        LOGGER.info("triggering stale payment recovery staleSeconds={}", staleSeconds);
        paymentProcessingService.reconcileStuckPayments(staleSeconds);
    }
}
