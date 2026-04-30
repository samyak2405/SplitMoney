package com.javaproject.splitewise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class UserReconciliationJob {

    private final UserReconciliationService reconciliationService;

    @Scheduled(fixedDelayString = "${user-reconciliation.fixed-delay-ms:300000}")
    public void run() {
        log.debug("user-reconciliation.job.started");
        try {
            reconciliationService.reconcile();
        } catch (Exception ex) {
            log.error("user-reconciliation.job.failed error={}", ex.getMessage(), ex);
        }
    }
}
