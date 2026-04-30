package com.javaproject.splitewise.model;

import com.javaproject.splitewise.model.converter.NumericStringAttributeConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"group", "user"})
@IdClass(GroupBalanceId.class)
public class GroupBalance {

    @Id
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Convert(converter = NumericStringAttributeConverter.class)
    @Column(name = "net_balance", nullable = false, precision = 18, scale = 2)
    private String netBalance;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", insertable = false, updatable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
