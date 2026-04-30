package com.javaproject.splitewise.service;

import com.javaproject.splitewise.service.impl.AddMemberToGroupService;
import com.javaproject.splitewise.service.impl.AddExpenseService;
import com.javaproject.splitewise.service.impl.CreateGroupService;
import com.javaproject.splitewise.service.impl.GetGroupDetailsService;
import com.javaproject.splitewise.service.impl.GetUserBalancesService;
import com.javaproject.splitewise.service.impl.GetUserGroupsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProcessorFactory {
    private final CreateGroupService createGroupService;
    private final AddMemberToGroupService addMemberToGroupService;
    private final GetGroupDetailsService getGroupDetailsService;
    private final GetUserGroupsService getUserGroupsService;
    private final GetUserBalancesService getUserBalancesService;
    private final AddExpenseService addExpenseService;
    private final SettleExpenseService settleExpenseService;
    public Processor getProcessor(String processorType){
        return switch(processorType){
            case "CREATE_GROUP"-> createGroupService;
            case "ADD_MEMBER_TO_GROUP" -> addMemberToGroupService;
            case "GET_GROUP_DETAILS" -> getGroupDetailsService;
            case "GET_USER_GROUPS" -> getUserGroupsService;
            case "GET_USER_BALANCES" -> getUserBalancesService;
            case "ADD_EXPENSE" -> addExpenseService;
            case "SETTLE_EXPENSE" -> settleExpenseService;
            default -> throw new IllegalStateException("Unexpected value: " + processorType);
        };
    }
}
