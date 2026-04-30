package com.javaproject.splitewise.LLD.strategy;

import com.javaproject.splitewise.LLD.model.User;

import java.util.Map;

public interface SplitwiseStrategy {

    Map<User, Long> split(User paidBy, long totalAmount, Map<User, Long> input);
}
