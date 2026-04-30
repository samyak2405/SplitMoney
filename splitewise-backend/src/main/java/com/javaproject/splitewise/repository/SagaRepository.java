package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.SagaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<SagaTransaction, UUID> {
    Optional<SagaTransaction> findByPaymentId(UUID paymentId);
}
