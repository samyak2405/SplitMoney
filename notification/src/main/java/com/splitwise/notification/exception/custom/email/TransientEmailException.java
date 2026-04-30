package com.splitwise.notification.exception.custom.email;

public class TransientEmailException extends RuntimeException {
    public TransientEmailException(String message) {
        super(message);
    }
}
