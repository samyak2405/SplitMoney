package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.ExpenseGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {
    boolean existsByGroupName(String groupName);

    Optional<ExpenseGroup> findByGroupName(@NotBlank(message = "group name is required") @Size(max = 120, message = "name must be at most 120 characters") String groupName);
}
