package com.splitmoney.ai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(schema = "aidb", name = "ai_interaction_log")
public class AiInteractionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "trigger_message", nullable = false, columnDefinition = "TEXT")
    private String triggerMessage;

    @Column(name = "stage_reached", nullable = false)
    private String stageReached;

    @Column(name = "expense_created", nullable = false)
    private boolean expenseCreated;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "paid_by_email")
    private String paidByEmail;

    @Column(name = "split_type", length = 16)
    private String splitType;

    @Column(name = "claude_turns", nullable = false)
    private int claudeTurns;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
