package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ForgotPasswordRequest;
import com.javaproject.application.exception.custom.ApiValidationException;
import com.javaproject.application.validator.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ForgotPasswordValidator implements Validator {

    @Override
    public void validateRequest(BaseRequest baseRequest) {
        ForgotPasswordRequest request = (ForgotPasswordRequest) baseRequest;
        boolean hasEmail  = request.getEmail()  != null && !request.getEmail().isBlank();
        boolean hasMobile = request.getMobile() != null && !request.getMobile().isBlank();
        if (!hasEmail && !hasMobile) {
            throw new ApiValidationException("Either email or mobile must be provided.", HttpStatus.BAD_REQUEST);
        }
    }
}
