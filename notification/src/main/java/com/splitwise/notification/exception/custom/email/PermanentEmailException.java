package com.splitwise.notification.exception.custom.email;

public class PermanentEmailException extends RuntimeException {
    public PermanentEmailException(String message) {
        super(message);
    }
}
