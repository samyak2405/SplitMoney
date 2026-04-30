package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.GroupBalance;
import com.javaproject.splitewise.model.GroupBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupBalanceRepository extends JpaRepository<GroupBalance, GroupBalanceId> {
    Optional<GroupBalance> findByGroupIdAndUserId(Long groupId, UUID userId);
    List<GroupBalance> findAllByGroupId(Long groupId);
}
