package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.persistence.entity.OutboxEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query("select o from OutboxEventEntity o where o.publishedAt is null order by o.createdAt asc")
    List<OutboxEventEntity> findUnpublished(Pageable pageable);

    @Query("update OutboxEventEntity o set o.publishedAt = :publishedAt where o.id = :id and o.publishedAt is null")
    @org.springframework.data.jpa.repository.Modifying
    @Transactional
    int markPublished(@Param("id") UUID id, @Param("publishedAt") Instant publishedAt);
}
