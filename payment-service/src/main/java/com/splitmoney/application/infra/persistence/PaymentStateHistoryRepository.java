package com.splitmoney.application.infra.persistence;

import com.splitmoney.application.domain.payment.PaymentStateHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStateHistoryRepository extends JpaRepository<PaymentStateHistory, UUID> {
}
