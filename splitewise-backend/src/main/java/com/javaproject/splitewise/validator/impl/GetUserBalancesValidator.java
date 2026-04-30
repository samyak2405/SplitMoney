package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetUserBalancesRequest;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class GetUserBalancesValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {
        if (!(request instanceof GetUserBalancesRequest getUserBalancesRequest)) {
            throw new ApiValidationException("Invalid request type for user-balances");
        }
        if (getUserBalancesRequest.getGroupName() == null || getUserBalancesRequest.getGroupName().isBlank()) {
            throw new ApiValidationException("groupName is required");
        }
        if (getUserBalancesRequest.getUserEmail() == null || getUserBalancesRequest.getUserEmail().isBlank()) {
            throw new ApiValidationException("userEmail is required");
        }
    }
}
