package com.splitwise.notification.service.email;

import com.splitwise.notification.exception.custom.email.PermanentEmailException;
import com.splitwise.notification.exception.custom.email.TransientEmailException;
import com.splitwise.notification.messaging.NotificationMessage;

public interface EmailGateway {
    void send(NotificationMessage message) throws TransientEmailException, PermanentEmailException;
}
