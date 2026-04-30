package com.splitmoney.application.infra.persistence;

import com.splitmoney.application.domain.webhook.ProcessedWebhook;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, UUID> {
    boolean existsByWebhookId(String webhookId);
}
