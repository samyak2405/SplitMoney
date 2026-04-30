package com.javaproject.splitewise.model;

import com.javaproject.splitewise.model.converter.NumericStringAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "expense_split",
        indexes = {
                @Index(name = "idx_expense_split_expense", columnList = "expense_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"expense", "user"})
@IdClass(ExpenseSplitId.class)
public class ExpenseSplit {

    @Id
    @Column(name = "expense_id", nullable = false)
    private Long expenseId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = NumericStringAttributeConverter.class)
    @Column(name = "share_amount", nullable = false, precision = 18, scale = 2)
    private String shareAmount;

    @Convert(converter = NumericStringAttributeConverter.class)
    @Column(name = "share_percentage", precision = 7, scale = 4)
    private String sharePercentage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
