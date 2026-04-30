package com.splitwise.notification.service.email;

import com.splitwise.notification.exception.custom.email.PermanentEmailException;
import com.splitwise.notification.exception.custom.email.TransientEmailException;
import com.splitwise.notification.messaging.NotificationMessage;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.email.provider", havingValue = "mock")
public class MockEmailGateway implements EmailGateway {
    private static final int RANDOM_BOUND = 100;
    private static final int PERMANENT_FAILURE_PERCENT = 2;
    private static final int TRANSIENT_FAILURE_PERCENT = 12;

    @Override
    public void send(NotificationMessage message) throws TransientEmailException, PermanentEmailException {
        Object forceType = message.payload().get("forceEmailError");
        if ("transient".equals(forceType)) {
            throw new TransientEmailException("Forced transient provider failure");
        }
        if ("permanent".equals(forceType)) {
            throw new PermanentEmailException("Forced permanent provider failure");
        }

        int chance = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
        if (chance < PERMANENT_FAILURE_PERCENT) {
            throw new PermanentEmailException("Invalid recipient email");
        }
        if (chance < TRANSIENT_FAILURE_PERCENT) {
            throw new TransientEmailException("Provider timeout");
        }
    }
}
