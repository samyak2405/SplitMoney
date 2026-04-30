package com.splitwise.notification.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "user_notification_status")
public class UserNotificationStatusEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID notificationId;

    @Column(nullable = false)
    private boolean isRead;

    @Column
    private Instant readAt;

    @Column(nullable = false)
    private boolean isClicked;

    @Column
    private Instant clickedAt;

    @Column(nullable = false)
    private Instant updatedAt;

}
