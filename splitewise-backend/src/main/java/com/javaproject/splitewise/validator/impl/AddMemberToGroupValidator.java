package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddMemberToGroupValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {

    }
}
