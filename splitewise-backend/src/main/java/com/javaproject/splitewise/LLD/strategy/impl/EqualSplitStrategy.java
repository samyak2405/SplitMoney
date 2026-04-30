package com.javaproject.splitewise.LLD.strategy.impl;

import com.javaproject.splitewise.LLD.model.User;
import com.javaproject.splitewise.LLD.strategy.SplitwiseStrategy;

import java.util.HashMap;
import java.util.Map;

public class EqualSplitStrategy implements SplitwiseStrategy {

    @Override
    public Map<User, Long> split(User paidBy, long totalAmount, Map<User, Long> input) {
        int count = input.size();
        long share = totalAmount / count;
        Map<User, Long> result = new HashMap<>();
        for (User user : input.keySet()) {
            result.put(user, share);
        }
        return result;
    }
}
