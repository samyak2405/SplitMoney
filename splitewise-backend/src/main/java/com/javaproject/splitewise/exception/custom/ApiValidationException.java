package com.javaproject.splitewise.exception.custom;

import org.springframework.http.HttpStatus;

public class ApiValidationException extends RuntimeException {

    private final HttpStatus status;

    public ApiValidationException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public ApiValidationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiValidationException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
