package com.javaproject.application.validator.factory;

import com.javaproject.application.validator.Validator;
import com.javaproject.application.validator.impl.ForgotPasswordValidator;
import com.javaproject.application.validator.impl.LoginRequestValidator;
import com.javaproject.application.validator.impl.RegisterUserValidator;
import com.javaproject.application.validator.impl.ResendRegistrationOtpValidator;
import com.javaproject.application.validator.impl.ResetPasswordValidator;
import com.javaproject.application.validator.impl.VerifyRegistrationOtpValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidatorFactory {

    private final RegisterUserValidator registerUserValidator;
    private final LoginRequestValidator loginRequestValidator;
    private final VerifyRegistrationOtpValidator verifyRegistrationOtpValidator;
    private final ResendRegistrationOtpValidator resendRegistrationOtpValidator;
    private final ForgotPasswordValidator forgotPasswordValidator;
    private final ResetPasswordValidator resetPasswordValidator;

    public Validator getValidator(String validatorType) {
        return switch (validatorType) {
            case "REGISTER"                -> registerUserValidator;
            case "LOGIN"                   -> loginRequestValidator;
            case "VERIFY_REGISTRATION_OTP" -> verifyRegistrationOtpValidator;
            case "RESEND_REGISTRATION_OTP" -> resendRegistrationOtpValidator;
            case "FORGOT_PASSWORD"         -> forgotPasswordValidator;
            case "RESET_PASSWORD"          -> resetPasswordValidator;
            default -> throw new IllegalArgumentException("invalid validator type: " + validatorType);
        };
    }
}
