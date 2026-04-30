package com.javaproject.splitewise.exception.custom;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;

public class UserNotInGroupException extends ProcessApiException {

    public UserNotInGroupException(String message) {
        super(message);
    }

    public UserNotInGroupException(String message, HttpStatus status) {
        super(message, status);
    }

    public UserNotInGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotInGroupException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
