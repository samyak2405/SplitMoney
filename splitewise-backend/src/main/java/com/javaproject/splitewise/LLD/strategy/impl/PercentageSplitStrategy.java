package com.javaproject.splitewise.LLD.strategy.impl;

import com.javaproject.splitewise.LLD.model.User;
import com.javaproject.splitewise.LLD.strategy.SplitwiseStrategy;

import java.util.HashMap;
import java.util.Map;

public class PercentageSplitStrategy implements SplitwiseStrategy {
    @Override
    public Map<User, Long> split(User paidBy, long totalAmount, Map<User, Long> input) {
        long sumPercent = input.values().stream().mapToLong(Long::longValue).sum();
        if(sumPercent!=100){
            throw new IllegalArgumentException("Percentage Split mismatch");
        }
        Map<User, Long> result = new HashMap<>();
        for(Map.Entry<User, Long> entry : input.entrySet()){
            long amount = (totalAmount*entry.getValue())/100;
            result.put(entry.getKey(), amount);
        }
        return result;
    }
}
