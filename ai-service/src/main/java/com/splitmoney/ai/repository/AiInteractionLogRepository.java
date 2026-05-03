package com.splitmoney.ai.repository;

import com.splitmoney.ai.model.AiInteractionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLog, Long> {
}
