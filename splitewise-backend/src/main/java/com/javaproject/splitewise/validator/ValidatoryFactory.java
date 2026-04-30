package com.javaproject.splitewise.validator;

import com.javaproject.splitewise.validator.impl.RemoveMemberFromGroupValidator;
import com.javaproject.splitewise.validator.impl.AddMemberToGroupValidator;
import com.javaproject.splitewise.validator.impl.AddExpenseValidator;
import com.javaproject.splitewise.validator.impl.CreateGroupValidator;
import com.javaproject.splitewise.validator.impl.GetGroupDetailsValidator;
import com.javaproject.splitewise.validator.impl.GetUserBalancesValidator;
import com.javaproject.splitewise.validator.impl.GetUserGroupsValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidatoryFactory {

    private final CreateGroupValidator createGroupValidator;
    private final AddMemberToGroupValidator addMemberToGroupValidator;
    private final GetGroupDetailsValidator getGroupDetailsValidator;
    private final GetUserGroupsValidator getUserGroupsValidator;
    private final GetUserBalancesValidator getUserBalancesValidator;
    private final AddExpenseValidator addExpenseValidator;
    private final RemoveMemberFromGroupValidator removeMemberFromGroupValidator;
    private final ExpenseSettlementValidator expenseSettlementValidator;
    public Validator getValidator(String validatorType){
        return switch (validatorType){
            case "CREATE_GROUP" -> createGroupValidator;
            case "ADD_MEMBER_TO_GROUP" -> addMemberToGroupValidator;
            case "REMOVE_MEMBER_FROM_GROUP" -> removeMemberFromGroupValidator;
            case "GET_GROUP_DETAILS" -> getGroupDetailsValidator;
            case "GET_USER_GROUPS" -> getUserGroupsValidator;
            case "GET_USER_BALANCES" -> getUserBalancesValidator;
            case "ADD_EXPENSE" -> addExpenseValidator;
            case "EXPENSE_SETTLEMENT" -> expenseSettlementValidator;
            default -> throw new IllegalArgumentException("invalid validator type"); };
    }
}
