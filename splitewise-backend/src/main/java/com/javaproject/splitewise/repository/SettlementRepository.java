package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findAllByGroupIdOrderBySettledAtDesc(Long groupId);
    List<Settlement> findAllByGroupIdAndPaidByUserId(Long groupId, UUID paidByUserId);
}
