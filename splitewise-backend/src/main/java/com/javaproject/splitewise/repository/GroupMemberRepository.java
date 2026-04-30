package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.GroupMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, UUID userId);

    boolean existsByGroupIdAndUserId(Long groupId, UUID userId);

    Optional<Integer> findByUserId(UUID userId);

    List<GroupMember> findAllByGroupId(Long groupId);

    List<GroupMember> findAllByUserId(UUID userId);

}
