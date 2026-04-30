package com.javaproject.splitewise.exception.custom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ProcessApiException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotFoundException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
