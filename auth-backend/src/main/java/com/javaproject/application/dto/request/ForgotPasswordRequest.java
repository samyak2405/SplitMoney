package com.javaproject.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest extends BaseRequest {

    @Email
    @Size(max = 255)
    private String email;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Mobile number must be in valid international format."
    )
    private String mobile;

    /** Preferred OTP delivery channel: EMAIL or SMS. Auto-resolved if blank. */
    private String otpChannel;
}
