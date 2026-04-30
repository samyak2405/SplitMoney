package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.NotificationOutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutboxEvent, UUID> {
    @Query("select o from NotificationOutboxEvent o where o.publishedAt is null order by o.createdAt asc")
    List<NotificationOutboxEvent> findUnpublished(Pageable pageable);

    @Modifying
    @Transactional
    @Query("update NotificationOutboxEvent o set o.publishedAt = :publishedAt where o.id = :id and o.publishedAt is null")
    int markPublished(@Param("id") UUID id, @Param("publishedAt") OffsetDateTime publishedAt);
}
