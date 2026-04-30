package com.splitmoney.application.infra.persistence;

import com.splitmoney.application.domain.outbox.OutboxEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);

    @Modifying
    @Query("update OutboxEvent e set e.published = true, e.publishedAt = :publishedAt where e.id = :id")
    int markPublished(@Param("id") UUID id, @Param("publishedAt") OffsetDateTime publishedAt);
}
