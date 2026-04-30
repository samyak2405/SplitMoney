package com.javaproject.splitewise.LLD;

import com.javaproject.splitewise.LLD.model.Expense;
import com.javaproject.splitewise.LLD.model.Group;
import com.javaproject.splitewise.LLD.model.Transaction;
import com.javaproject.splitewise.LLD.model.User;
import com.javaproject.splitewise.LLD.service.SettlementService;
import com.javaproject.splitewise.LLD.strategy.SplitwiseStrategy;
import com.javaproject.splitewise.LLD.strategy.impl.EqualSplitStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        User a = new User("1","Alice");
        User b = new User("2","Bob");
        User c = new User("3","Charlie");

        Group group = new Group("group1");
        group.addMember(a);
        group.addMember(b);
        group.addMember(c);

        SplitwiseStrategy strategy = new EqualSplitStrategy();
        Map<User, Long> participants = new HashMap<>();
        participants.put(a,0L);
        participants.put(b,0L);
        participants.put(c,0L);

        Map<User, Long> split = strategy.split(a, 3000L, participants);
        Expense expense = new Expense(a, 3000L, split);
        group.getBalanceSheet().applyExpense(expense);
        Map<User, Long> balances = group.getBalanceSheet().getSnapshot();
        SettlementService service = new SettlementService();
        List<Transaction> txns = service.settleOptimal(balances);
        txns.forEach(System.out::println);
    }
}
