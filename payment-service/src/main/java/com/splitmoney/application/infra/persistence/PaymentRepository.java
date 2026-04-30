package com.splitmoney.application.infra.persistence;

import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.domain.payment.PaymentStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByClientRequestId(String clientRequestId);

    Optional<Payment> findByHyperswitchPaymentId(String hyperswitchPaymentId);

    @Modifying
    @Query("""
            update Payment p
            set p.status = :newStatus,
                p.updatedAt = :updatedAt
            where p.id = :id
              and p.status = :expectedStatus
              and p.version = :expectedVersion
            """)
    int transitionState(
            @Param("id") UUID id,
            @Param("expectedStatus") PaymentStatus expectedStatus,
            @Param("newStatus") PaymentStatus newStatus,
            @Param("expectedVersion") long expectedVersion,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    List<Payment> findTop100ByStatusInAndUpdatedAtBefore(List<PaymentStatus> statuses, OffsetDateTime before);

    @Query("""
            select p from Payment p
            where (p.payerUserId = :userId or p.payeeUserId = :userId)
              and (:status is null or p.status = :status)
              and (:cursor is null or p.createdAt < :cursor)
            order by p.createdAt desc
            limit :limit
            """)
    List<Payment> listForUser(
            @Param("userId") UUID userId,
            @Param("status") PaymentStatus status,
            @Param("cursor") OffsetDateTime cursor,
            @Param("limit") int limit
    );
}
