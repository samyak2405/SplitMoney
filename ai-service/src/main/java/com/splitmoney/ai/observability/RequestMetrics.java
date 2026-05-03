package com.splitmoney.ai.observability;

/**
 * Thread-local accumulator for AI provider call metrics within a single HTTP request.
 * Providers write here; SplityController reads at the end of each chat request.
 */
public final class RequestMetrics {

    private RequestMetrics() {}

    private static final ThreadLocal<Snapshot> HOLDER = ThreadLocal.withInitial(Snapshot::new);

    public static void clear() {
        HOLDER.remove();
    }

    public static void record(String model, int inputTokens, int outputTokens,
                               long latencyMs, boolean isError) {
        Snapshot s = HOLDER.get();
        s.totalInputTokens += inputTokens;
        s.totalOutputTokens += outputTokens;
        s.totalAiLatencyMs += latencyMs;
        s.aiCalls++;
        if (isError) s.aiErrors++;
        if (model != null) s.model = model;
    }

    /** Returns the accumulated snapshot and removes it from the ThreadLocal. */
    public static Snapshot getAndClear() {
        Snapshot s = HOLDER.get();
        HOLDER.remove();
        return s;
    }

    public static class Snapshot {
        public int totalInputTokens;
        public int totalOutputTokens;
        public long totalAiLatencyMs;
        public int aiCalls;
        public int aiErrors;
        public String model = "unknown";
    }
}
