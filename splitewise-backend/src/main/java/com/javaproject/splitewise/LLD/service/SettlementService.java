package com.javaproject.splitewise.LLD.service;

import com.javaproject.splitewise.LLD.model.Transaction;
import com.javaproject.splitewise.LLD.model.User;

import java.util.*;

public class SettlementService {

    public List<Transaction> settleOptimal(Map<User, Long> balances){
        PriorityQueue<Map.Entry<User,Long>> creditors =
                new PriorityQueue<>((a,b)->Long.compare(b.getValue(),a.getValue()));
        PriorityQueue<Map.Entry<User,Long>> debtors =
                new PriorityQueue<>((a,b)->Long.compare(Math.abs(b.getValue()),Math.abs(a.getValue())));

        for (Map.Entry<User, Long> entry:balances.entrySet()){
            if(entry.getValue()>0){
                creditors.add(entry);
            }else if(entry.getValue()<0){
                debtors.add(entry);
            }
        }
        List<Transaction> transactions = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()){
            Map.Entry<User,Long> creditor = creditors.poll();
            Map.Entry<User,Long> debtor = debtors.poll();

            long settleAmount = Math.min(creditor.getValue(), -debtor.getValue());

            transactions.add(new Transaction(debtor.getKey(), creditor.getKey(), settleAmount));
            long newCredit = creditor.getValue() - settleAmount;
            long newDebt = debtor.getValue() + settleAmount;

            if(newCredit > 0){
                creditors.add(new AbstractMap.SimpleEntry<>(creditor.getKey(), newCredit ));
            }
            if(newDebt<0){
                debtors.add(new AbstractMap.SimpleEntry<>(debtor.getKey(), newDebt ));
            }
        }
        return transactions;
    }
}
