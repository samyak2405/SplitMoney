package com.splitmoney.ai.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget HTTP client for the Langfuse v2 ingestion API.
 * All sends use .subscribe() so they never block the calling thread.
 */
@Slf4j
@Component
public class LangfuseClient {

    private final WebClient webClient;
    private final boolean enabled;

    public LangfuseClient(LangfuseProperties props) {
        this.enabled = props.isEnabled()
                && props.getPublicKey() != null && !props.getPublicKey().isBlank()
                && props.getSecretKey() != null && !props.getSecretKey().isBlank();

        String credentials = props.getPublicKey() + ":" + props.getSecretKey();
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        this.webClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", basicAuth)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();

        if (enabled) {
            log.info("LangfuseClient initialised baseUrl={}", props.getBaseUrl());
        } else {
            log.warn("LangfuseClient: disabled or missing keys — observability events will be skipped");
        }
    }

    /**
     * Ingests a batch of Langfuse events (trace, generation, score).
     * The call is fire-and-forget: errors are logged but never thrown.
     */
    public void ingest(List<Map<String, Object>> events) {
        if (!enabled || events == null || events.isEmpty()) return;
        Map<String, Object> body = Map.of("batch", events);
        log.debug("LangfuseClient.ingest events={}", events.size());
        webClient.post()
                .uri("/api/public/ingestion")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        resp -> log.debug("Langfuse ingest OK events={}", events.size()),
                        err  -> log.warn("Langfuse ingest failed: {}", err.getMessage())
                );
    }

    // ── Event builders ──────────────────────────────────────────────────────

    public Map<String, Object> traceEvent(String traceId, String name, String userId,
                                           String sessionId, Object input, Object output,
                                           Map<String, Object> metadata, List<String> tags) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", traceId);
        body.put("name", name);
        body.put("userId", userId);
        body.put("sessionId", sessionId);
        body.put("input", input);
        body.put("output", output);
        body.put("metadata", metadata);
        body.put("tags", tags);
        return eventWrapper("trace-create", body);
    }

    public Map<String, Object> generationEvent(String traceId, String model,
                                                 int inputTokens, int outputTokens,
                                                 long latencyMs, Object input, Object output) {
        String genId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", genId);
        body.put("traceId", traceId);
        body.put("name", "llm-call");
        body.put("model", model);
        body.put("startTime", now.minusMillis(latencyMs).toString());
        body.put("endTime", now.toString());
        body.put("input", input);
        body.put("output", output);
        body.put("usage", Map.of(
                "input", inputTokens,
                "output", outputTokens,
                "unit", "TOKENS"
        ));
        return eventWrapper("generation-create", body);
    }

    public Map<String, Object> scoreEvent(String traceId, String name, double value, String comment) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("traceId", traceId);
        body.put("name", name);
        body.put("value", value);
        body.put("dataType", "NUMERIC");
        body.put("comment", comment);
        return eventWrapper("score-create", body);
    }

    private Map<String, Object> eventWrapper(String type, Map<String, Object> body) {
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "type", type,
                "timestamp", Instant.now().toString(),
                "body", body
        );
    }
}
