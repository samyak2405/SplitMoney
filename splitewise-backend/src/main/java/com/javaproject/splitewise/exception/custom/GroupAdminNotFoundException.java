package com.javaproject.splitewise.exception.custom;

import org.springframework.http.HttpStatus;

public class GroupAdminNotFoundException extends ProcessApiException{

    public GroupAdminNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public GroupAdminNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }

    public GroupAdminNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupAdminNotFoundException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
