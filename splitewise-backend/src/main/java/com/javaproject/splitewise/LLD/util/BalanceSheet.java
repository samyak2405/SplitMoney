package com.javaproject.splitewise.LLD.util;

import com.javaproject.splitewise.LLD.model.Expense;
import com.javaproject.splitewise.LLD.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class BalanceSheet {
    private final Map<User, Long> netBalances = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void applyExpense(Expense expense) {
        lock.lock();
        try {
            User paidBy = expense.getPaidBy();
            netBalances.putIfAbsent(paidBy, 0L);
            for(Map.Entry<User, Long> entry : expense.getSplitAmounts().entrySet()) {
                User user = entry.getKey();
                long amount = entry.getValue();
                netBalances.putIfAbsent(user, 0L);
                netBalances.put(user, netBalances.get(user) - amount);
                netBalances.put(paidBy, netBalances.get(paidBy) + amount);
            }
        }finally {
            lock.unlock();
        }
    }

    public Map<User, Long> getSnapshot() {
        lock.lock();
        try {
            return new HashMap<>(netBalances);
        } finally {
            lock.unlock();
        }
    }
}
