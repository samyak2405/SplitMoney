package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetGroupDetailsRequest;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Component
public class GetGroupDetailsValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {
        if (!(request instanceof GetGroupDetailsRequest getGroupDetailsRequest)) {
            throw new ApiValidationException("Invalid request type for group-details");
        }
        if (!StringUtils.hasText(getGroupDetailsRequest.getGroupName())) {
            throw new ApiValidationException("groupName is required");
        }
        if (getGroupDetailsRequest.getUserId() == null) {
            throw new ApiValidationException("userId is required");
        }
    }
}
