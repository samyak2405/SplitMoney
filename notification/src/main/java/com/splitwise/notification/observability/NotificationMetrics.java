package com.splitwise.notification.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {
    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void workerProcessed(String channel) {
        counter("notification.worker.processed", channel).increment();
    }

    public void workerRetried(String channel) {
        counter("notification.worker.retried", channel).increment();
    }

    public void workerFailed(String channel) {
        counter("notification.worker.failed", channel).increment();
    }

    public void workerDlqPublished(String channel) {
        counter("notification.worker.dlq.published", channel).increment();
    }

    private Counter counter(String name, String channel) {
        return Counter.builder(name)
                .tag("channel", channel)
                .register(meterRegistry);
    }
}
