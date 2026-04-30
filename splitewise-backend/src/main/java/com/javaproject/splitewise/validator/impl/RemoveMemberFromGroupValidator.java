package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RemoveMemberFromGroupValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {

    }
}
