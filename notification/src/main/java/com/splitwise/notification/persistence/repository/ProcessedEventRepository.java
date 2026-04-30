package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.persistence.entity.ProcessedEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, UUID> {
    boolean existsByDedupeKey(String dedupeKey);
}
