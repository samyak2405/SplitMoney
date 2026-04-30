package com.splitwise.notification.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false)
    private Instant processedAt;

    @Column(nullable = false, length = 200)
    private String dedupeKey;
}
