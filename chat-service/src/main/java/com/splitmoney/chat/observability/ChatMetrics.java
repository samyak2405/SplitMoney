package com.splitmoney.chat.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ChatMetrics {

    private final MeterRegistry registry;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public ChatMetrics(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("chat.websocket.connections.active", activeConnections, AtomicInteger::get)
                .description("Currently active WebSocket connections")
                .register(registry);
    }

    // ── WebSocket lifecycle ──────────────────────────────────────────────────

    public void wsConnected() { activeConnections.incrementAndGet(); }
    public void wsDisconnected() { activeConnections.decrementAndGet(); }

    // ── Message flow ─────────────────────────────────────────────────────────

    public void recordMessageSent(String msgType) {
        Counter.builder("chat.messages.sent")
                .tag("msg_type", msgType)
                .register(registry).increment();
    }

    public void recordKafkaPublished() {
        Counter.builder("chat.kafka.published").register(registry).increment();
    }

    public void recordKafkaPublishFailed() {
        Counter.builder("chat.kafka.publish.failed").register(registry).increment();
    }

    public void recordKafkaConsumed(String msgType) {
        Counter.builder("chat.kafka.consumed")
                .tag("msg_type", msgType)
                .register(registry).increment();
    }

    /** Measures end-to-end latency: from WebSocket send (message creation) to Kafka consumer delivery. */
    public void recordMessageE2eLatency(long createdAtEpochMs) {
        long latencyMs = System.currentTimeMillis() - createdAtEpochMs;
        Timer.builder("chat.message.e2e.latency")
                .register(registry)
                .record(Duration.ofMillis(Math.max(0, latencyMs)));
    }

    // ── REST API ─────────────────────────────────────────────────────────────

    public void recordHistoryFetch(boolean cursorBased) {
        Counter.builder("chat.history.fetches")
                .tag("cursor_based", String.valueOf(cursorBased))
                .register(registry).increment();
    }

    public void recordMarkRead() {
        Counter.builder("chat.mark.read").register(registry).increment();
    }

    // ── Presence & engagement ─────────────────────────────────────────────────

    public void recordHeartbeat() {
        Counter.builder("chat.heartbeats").register(registry).increment();
    }

    /** Called only when typing starts (first frame per TTL window). */
    public void recordTypingEvent() {
        Counter.builder("chat.typing.events").register(registry).increment();
    }

    /** status = "ONLINE" or "OFFLINE". */
    public void recordPresenceTransition(String status) {
        Counter.builder("chat.presence.transitions")
                .tag("status", status)
                .register(registry).increment();
    }
}
