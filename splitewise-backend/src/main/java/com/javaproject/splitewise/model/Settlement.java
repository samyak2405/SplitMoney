package com.javaproject.splitewise.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "settlement",
        indexes = {
                @Index(name = "idx_settlement_group",    columnList = "group_id,settled_at"),
                @Index(name = "idx_settlement_paid_by",  columnList = "paid_by_user_id,group_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "paid_by_user_id", nullable = false)
    private UUID paidByUserId;

    @Column(name = "paid_to_user_id", nullable = false)
    private UUID paidToUserId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private java.math.BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "notes")
    private String notes;

    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
