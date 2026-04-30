package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetUserGroupsRequest;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class GetUserGroupsValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {
        if (!(request instanceof GetUserGroupsRequest getUserGroupsRequest)) {
            throw new ApiValidationException("Invalid request type for user-groups");
        }
        if (getUserGroupsRequest.getUserId() == null) {
            throw new ApiValidationException("userId is required");
        }
    }
}
