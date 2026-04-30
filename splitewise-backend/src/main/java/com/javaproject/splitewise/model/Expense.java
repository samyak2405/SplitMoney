package com.javaproject.splitewise.model;

import com.javaproject.splitewise.model.converter.NumericStringAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "expense",
        indexes = {
                @Index(name = "idx_expense_group_date_desc", columnList = "group_id,expense_date,expense_id"),
                @Index(name = "idx_expense_group_payer_date_desc", columnList = "group_id,paid_by_user_id,expense_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"group", "paidBy", "createdBy"})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "paid_by_user_id", nullable = false)
    private UUID paidByUserId;

    @Convert(converter = NumericStringAttributeConverter.class)
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private String totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "description", nullable = false, length = 280)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 16)
    private SplitType splitType;

    @Column(name = "expense_date", nullable = false)
    private LocalDateTime expenseDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id", insertable = false, updatable = false)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", insertable = false, updatable = false)
    private User createdBy;
}
