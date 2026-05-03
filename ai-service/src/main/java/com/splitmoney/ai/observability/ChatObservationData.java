package com.splitmoney.ai.observability;

/**
 * Immutable snapshot of everything collected during a single Splity chat request,
 * passed to ObservabilityService after the AI response has been generated.
 */
public record ChatObservationData(
        String traceId,
        String userId,
        String userEmail,
        String groupId,
        String userMessage,
        String replyText,
        boolean expenseCreated,
        boolean cancelled,
        boolean hasDocument,
        String mimeType,
        RequestMetrics.Snapshot aiMetrics,
        long requestLatencyMs,
        int conversationTurns
) {
    public String outcomeTag() {
        if (expenseCreated) return "outcome:expense_created";
        if (cancelled) return "outcome:cancelled";
        if (aiMetrics.aiErrors > 0) return "outcome:error";
        return "outcome:chat";
    }

    public String outcomeLabel() {
        if (expenseCreated) return "expense_created";
        if (cancelled) return "cancelled";
        if (aiMetrics.aiErrors > 0) return "error";
        return "chat";
    }
}
