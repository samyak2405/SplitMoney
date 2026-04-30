package com.javaproject.splitewise.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_transactions")
@Getter
@Setter
public class SagaTransaction {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private UUID payerUserId;

    @Column(nullable = false)
    private UUID payeeUserId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String paymentMethod;

    private UUID paymentId;

    @Column(length = 1024)
    private String checkoutUrl;

    /** VALIDATION_PASSED | PAYMENT_INITIATED | PAYMENT_COMPLETED |
     *  SETTLEMENT_CONFIRMED | PAYMENT_FAILED | COMPENSATED */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String errorReason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
