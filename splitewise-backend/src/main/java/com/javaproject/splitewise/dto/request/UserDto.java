package com.javaproject.splitewise.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    @Pattern(
            regexp = "^$|^\\+?[1-9]\\d{7,14}$",
            message = "phoneNumber must be a valid E.164 number"
    )
    private String phoneNumber;
    private Long exactAmount;
    private float percentage;
}
