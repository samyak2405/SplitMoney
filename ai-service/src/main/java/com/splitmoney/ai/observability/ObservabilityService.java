package com.splitmoney.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Records Prometheus metrics synchronously and ships Langfuse trace/generation/score
 * events asynchronously so the HTTP response is never delayed.
 *
 * Call order from SplityController:
 *   1. recordMetrics(data)   — synchronous, fast
 *   2. sendToLangfuse(data)  — @Async, fire-and-forget
 */
@Slf4j
@Service
public class ObservabilityService {

    private static final String PROMPT_VERSION = "1.0";

    // Prometheus metrics
    private final Timer requestTimer;
    private final MeterRegistry registry;
    private final LangfuseClient langfuseClient;
    private final String environment;

    // Bad-answer thresholds
    private static final int EXCESSIVE_TURNS_THRESHOLD = 8;

    public ObservabilityService(MeterRegistry registry, LangfuseClient langfuseClient,
                                 @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.registry = registry;
        this.langfuseClient = langfuseClient;
        this.environment = activeProfile;
        this.requestTimer = Timer.builder("splity.request.duration.seconds")
                .description("End-to-end chat request duration")
                .publishPercentileHistogram(true)
                .register(registry);
    }

    // ── Synchronous metrics ──────────────────────────────────────────────────

    public void recordMetrics(ChatObservationData data) {
        // Request outcome counter
        Counter.builder("splity.chat.requests.total")
                .tag("provider", normaliseProvider(data.aiMetrics().model))
                .tag("outcome", data.outcomeLabel())
                .description("Total Splity chat requests by outcome")
                .register(registry)
                .increment();

        // End-to-end latency
        requestTimer.record(data.requestLatencyMs(), TimeUnit.MILLISECONDS);

        // Document processed
        if (data.hasDocument() && data.mimeType() != null) {
            Counter.builder("splity.document.processed.total")
                    .tag("mimeType", data.mimeType())
                    .description("Bill/receipt documents processed by Splity")
                    .register(registry)
                    .increment();
        }

        // Bad answer flags
        String badReason = detectBadAnswer(data);
        if (badReason != null) {
            Counter.builder("splity.bad.answers.total")
                    .tag("reason", badReason)
                    .description("Splity responses flagged as bad answers")
                    .register(registry)
                    .increment();
            log.warn("Bad answer flagged traceId={} reason={} turns={} aiErrors={}",
                    data.traceId(), badReason, data.conversationTurns(), data.aiMetrics().aiErrors);
        }
    }

    // ── Async Langfuse pipeline ──────────────────────────────────────────────

    @Async
    public void sendToLangfuse(ChatObservationData data) {
        try {
            log.debug("sendToLangfuse traceId={} outcome={}", data.traceId(), data.outcomeLabel());
            List<Map<String, Object>> events = new ArrayList<>();

            // 1. Trace
            Map<String, Object> traceMeta = new LinkedHashMap<>();
            traceMeta.put("groupId", data.groupId());
            traceMeta.put("model", data.aiMetrics().model);
            traceMeta.put("promptVersion", PROMPT_VERSION);
            traceMeta.put("temperature", "default");
            traceMeta.put("environment", environment);
            traceMeta.put("hasDocument", data.hasDocument());
            traceMeta.put("aiTurns", data.conversationTurns());
            traceMeta.put("aiCalls", data.aiMetrics().aiCalls);
            traceMeta.put("inputTokens", data.aiMetrics().totalInputTokens);
            traceMeta.put("outputTokens", data.aiMetrics().totalOutputTokens);
            traceMeta.put("aiLatencyMs", data.aiMetrics().totalAiLatencyMs);
            traceMeta.put("requestLatencyMs", data.requestLatencyMs());

            List<String> tags = new ArrayList<>();
            tags.add("provider:" + normaliseProvider(data.aiMetrics().model));
            tags.add("env:" + environment);
            tags.add(data.outcomeTag());
            if (data.hasDocument()) tags.add("has_document");

            String badReason = detectBadAnswer(data);
            if (badReason != null) tags.add("flag:" + badReason);

            events.add(langfuseClient.traceEvent(
                    data.traceId(),
                    "splity-chat",
                    data.userEmail(),
                    data.groupId() + ":" + data.userId(),
                    Map.of("message", truncate(data.userMessage(), 500)),
                    Map.of("message", truncate(data.replyText(), 500)),
                    traceMeta,
                    tags
            ));

            // 2. Generation (if at least one AI call was made)
            if (data.aiMetrics().aiCalls > 0) {
                events.add(langfuseClient.generationEvent(
                        data.traceId(),
                        data.aiMetrics().model,
                        data.aiMetrics().totalInputTokens,
                        data.aiMetrics().totalOutputTokens,
                        data.aiMetrics().totalAiLatencyMs,
                        Map.of("message", truncate(data.userMessage(), 500)),
                        Map.of("message", truncate(data.replyText(), 500))
                ));
            }

            // 3. Outcome score
            double outcomeScore = data.expenseCreated() ? 1.0
                    : data.cancelled() ? 0.0
                    : data.aiMetrics().aiErrors > 0 ? 0.0
                    : 0.5;
            String outcomeComment = data.expenseCreated() ? "Expense created successfully"
                    : data.cancelled() ? "User cancelled"
                    : data.aiMetrics().aiErrors > 0 ? "AI API error"
                    : "Ongoing conversation";
            events.add(langfuseClient.scoreEvent(data.traceId(), "outcome_score",
                    outcomeScore, outcomeComment));

            // 4. Quality flag score (if bad answer detected)
            if (badReason != null) {
                events.add(langfuseClient.scoreEvent(data.traceId(), "quality_flag",
                        0.0, "Bad answer: " + badReason));
            }

            // 5. Cost efficiency score (tokens consumed — lower is better)
            int totalTokens = data.aiMetrics().totalInputTokens + data.aiMetrics().totalOutputTokens;
            if (totalTokens > 0) {
                // Normalise: 0 = excellent (few tokens), scaled down with more tokens
                double costScore = Math.max(0.0, 1.0 - (totalTokens / 5000.0));
                events.add(langfuseClient.scoreEvent(data.traceId(), "cost_efficiency",
                        costScore, "Total tokens: " + totalTokens));
            }

            langfuseClient.ingest(events);

        } catch (Exception e) {
            log.warn("sendToLangfuse failed traceId={}: {}", data.traceId(), e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String detectBadAnswer(ChatObservationData data) {
        if (data.aiMetrics().aiErrors > 0) return "api_error";
        if (data.conversationTurns() > EXCESSIVE_TURNS_THRESHOLD) return "excessive_turns";
        if (data.aiMetrics().aiCalls > 0 && data.replyText() == null) return "null_response";
        return null;
    }

    private String normaliseProvider(String model) {
        if (model == null) return "unknown";
        if (model.startsWith("claude")) return "claude";
        if (model.startsWith("gpt") || model.startsWith("openai")) return "openai";
        return model;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
