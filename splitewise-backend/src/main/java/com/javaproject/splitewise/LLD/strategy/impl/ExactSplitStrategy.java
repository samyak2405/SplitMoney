package com.javaproject.splitewise.LLD.strategy.impl;

import com.javaproject.splitewise.LLD.model.User;
import com.javaproject.splitewise.LLD.strategy.SplitwiseStrategy;

import java.util.Map;

public class ExactSplitStrategy implements SplitwiseStrategy {


    @Override
    public Map<User, Long> split(User paidBy, long totalAmount, Map<User, Long> input) {
        long sum = input.values().stream().mapToLong(Long::longValue).sum();
        if(sum!=totalAmount) {
            throw new IllegalArgumentException("Exact Split mismatch");
        }
        return input;
    }
}
