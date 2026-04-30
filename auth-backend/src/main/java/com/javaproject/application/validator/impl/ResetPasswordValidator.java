package com.javaproject.application.validator.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResetPasswordRequest;
import com.javaproject.application.exception.custom.ApiValidationException;
import com.javaproject.application.validator.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetPasswordValidator implements Validator {

    @Override
    public void validateRequest(BaseRequest baseRequest) {
        ResetPasswordRequest request = (ResetPasswordRequest) baseRequest;

        boolean hasEmail  = request.getEmail()  != null && !request.getEmail().isBlank();
        boolean hasMobile = request.getMobile() != null && !request.getMobile().isBlank();
        if (!hasEmail && !hasMobile) {
            throw new ApiValidationException("Either email or mobile must be provided.", HttpStatus.BAD_REQUEST);
        }
        if (request.getOtp() == null || !request.getOtp().matches("\\d{6}")) {
            throw new ApiValidationException("OTP must be exactly 6 digits.", HttpStatus.BAD_REQUEST);
        }
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new ApiValidationException("New password is required.", HttpStatus.BAD_REQUEST);
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiValidationException("Passwords do not match.", HttpStatus.BAD_REQUEST);
        }
    }
}
