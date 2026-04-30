package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByActorUserIdAndEndpointAndIdemKey(UUID actorUserId, String endpoint, String idemKey);
}
