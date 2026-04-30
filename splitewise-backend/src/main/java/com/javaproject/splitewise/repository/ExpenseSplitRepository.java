package com.javaproject.splitewise.repository;

import com.javaproject.splitewise.model.ExpenseSplit;
import com.javaproject.splitewise.model.ExpenseSplitId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, ExpenseSplitId> {
}
