package com.javaproject.splitewise.LLD.model;

import com.javaproject.splitewise.LLD.util.BalanceSheet;

import java.util.HashSet;
import java.util.Set;

public class Group {
    private final String id;
    private final Set<User> members = new HashSet<>();
    private final BalanceSheet balanceSheet = new  BalanceSheet();
    public Group(String id) {
        this.id = id;
    }

    public void addMember(User user) {
        members.add(user);
    }
    public BalanceSheet getBalanceSheet() {
        return balanceSheet;
    }
}
