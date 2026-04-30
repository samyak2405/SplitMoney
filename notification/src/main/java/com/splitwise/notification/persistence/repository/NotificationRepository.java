package com.splitwise.notification.persistence.repository;

import com.splitwise.notification.persistence.entity.NotificationEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    @Query("""
            select n
            from NotificationEntity n
            where n.userId = :userId
            and (n.createdAt < :cursorCreatedAt or (n.createdAt = :cursorCreatedAt and n.id < :cursorId))
            order by n.createdAt desc, n.id desc
            """)
    List<NotificationEntity> findByUserIdWithCursor(
            @Param("userId") UUID userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
