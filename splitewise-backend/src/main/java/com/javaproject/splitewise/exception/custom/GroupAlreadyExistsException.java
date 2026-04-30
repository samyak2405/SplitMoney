package com.javaproject.splitewise.exception.custom;

import org.springframework.http.HttpStatus;

public class GroupAlreadyExistsException extends ProcessApiException {

    public GroupAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }

    public GroupAlreadyExistsException(String message, HttpStatus status) {
        super(message, status);
    }

    public GroupAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupAlreadyExistsException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
