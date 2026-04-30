package com.javaproject.splitewise.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExpenseSplitId implements Serializable {
    private Long expenseId;
    private UUID userId;
}
