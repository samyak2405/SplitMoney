package com.javaproject.splitewise.exception.custom;

import org.springframework.http.HttpStatus;

public class GroupNotExistsException extends ProcessApiException {

    public GroupNotExistsException(String message) {
        super(message);
    }

    public GroupNotExistsException(String message, HttpStatus status) {
        super(message, status);
    }

    public GroupNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupNotExistsException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
