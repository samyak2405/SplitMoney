package com.javaproject.splitewise.LLD.model;

public class Transaction {
    private final User from;
    private final User to;
    private final long amount;

    public Transaction(User from, User to, long amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return from.toString() + " " + to.toString() + " " + amount;
    }
}
