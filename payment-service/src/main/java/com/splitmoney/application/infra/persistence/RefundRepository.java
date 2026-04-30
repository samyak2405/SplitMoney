package com.splitmoney.application.infra.persistence;

import com.splitmoney.application.domain.refund.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByPaymentId(UUID paymentId);
    Optional<Refund> findByHyperswitchRefundId(String hyperswitchRefundId);
}
