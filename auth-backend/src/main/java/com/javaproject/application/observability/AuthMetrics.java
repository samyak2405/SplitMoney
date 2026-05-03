package com.javaproject.application.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final MeterRegistry registry;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordLogin(String method, String outcome) {
        Counter.builder("auth.login")
                .tag("method", method)     // password | google | otp
                .tag("outcome", outcome)   // success | bad_credentials | account_locked | otp_required
                .description("Login attempts by method and outcome")
                .register(registry)
                .increment();
    }

    public void recordJwtIssued(String type) {
        Counter.builder("auth.jwt.issued")
                .tag("type", type)         // access | refresh
                .description("JWTs issued")
                .register(registry)
                .increment();
    }

    public void recordRegistration(String outcome) {
        Counter.builder("auth.registration")
                .tag("outcome", outcome)   // initiated | otp_verified | failed
                .description("User registration events")
                .register(registry)
                .increment();
    }

    public void recordPasswordReset(String outcome) {
        Counter.builder("auth.password_reset")
                .tag("outcome", outcome)   // requested | completed | failed
                .description("Password reset events")
                .register(registry)
                .increment();
    }

    public void recordEventPublished(String eventType) {
        Counter.builder("auth.events.published")
                .tag("event_type", eventType)
                .description("RabbitMQ events published by auth-backend")
                .register(registry)
                .increment();
    }
}
