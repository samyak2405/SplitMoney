package com.javaproject.splitewise.LLD.model;

import java.util.Map;

public class Expense {
    private final User paidBy;
    private final long amount;
    private final Map<User, Long> splitAmounts;

    public Expense(User paidBy, long amount, Map<User, Long> splitAmounts) {
        this.paidBy = paidBy;
        this.amount = amount;
        this.splitAmounts = splitAmounts;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public long getAmount() {
        return amount;
    }

    public Map<User, Long> getSplitAmounts() {
        return splitAmounts;
    }
}

