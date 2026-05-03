package com.splitmoney.ai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmoney.ai.conversation.ConversationState.ChatMessage;
import com.splitmoney.ai.observability.RequestMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeAiProvider implements AiChatProvider {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final Timer aiCallTimer;
    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;
    private final Counter errorCounter;

    public ClaudeAiProvider(
            @Value("${ai.claude.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${ai.claude.api-key}") String apiKey,
            @Value("${ai.claude.model:claude-haiku-4-5-20251001}") String model,
            @Value("${ai.claude.max-tokens:1500}") int maxTokens,
            MeterRegistry meterRegistry
    ) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.aiCallTimer = Timer.builder("splity.ai.call.duration.seconds")
                .tag("provider", "claude")
                .description("Claude API call duration")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        this.inputTokenCounter = Counter.builder("splity.ai.tokens.total")
                .tag("provider", "claude").tag("direction", "input")
                .description("Claude input tokens consumed")
                .register(meterRegistry);
        this.outputTokenCounter = Counter.builder("splity.ai.tokens.total")
                .tag("provider", "claude").tag("direction", "output")
                .description("Claude output tokens produced")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("splity.ai.errors.total")
                .tag("provider", "claude")
                .description("Claude API errors")
                .register(meterRegistry);
    }

    @Override
    public String chat(List<ChatMessage> history, String systemPrompt) {
        List<Map<String, String>> messages = history.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", messages
        );

        log.debug("Calling Claude model={} messageCount={}", model, messages.size());
        long start = System.currentTimeMillis();

        try {
            JsonNode response = webClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            long latencyMs = System.currentTimeMillis() - start;

            if (response == null || !response.has("content")) {
                log.error("Claude returned null or missing content. Response: {}", response);
                errorCounter.increment();
                RequestMetrics.record(model, 0, 0, latencyMs, true);
                return null;
            }

            String stopReason = response.path("stop_reason").asText(null);
            if ("max_tokens".equals(stopReason)) {
                log.warn("Claude response truncated by max_tokens={} — increase ai.claude.max-tokens", maxTokens);
            }

            int inputTokens = response.path("usage").path("input_tokens").asInt(0);
            int outputTokens = response.path("usage").path("output_tokens").asInt(0);
            aiCallTimer.record(latencyMs, TimeUnit.MILLISECONDS);
            inputTokenCounter.increment(inputTokens);
            outputTokenCounter.increment(outputTokens);
            RequestMetrics.record(model, inputTokens, outputTokens, latencyMs, false);

            JsonNode contentArr = response.get("content");
            if (!contentArr.isArray() || contentArr.isEmpty()) return null;

            String text = contentArr.get(0).path("text").asText(null);
            log.info("Claude response stopReason={} length={} inputTokens={} outputTokens={} latencyMs={}",
                    stopReason, text == null ? 0 : text.length(), inputTokens, outputTokens, latencyMs);
            return text;

        } catch (WebClientResponseException e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("Claude API error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("Claude API call failed: {}", e.getMessage());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        }
    }

    @Override
    public String chatWithDocument(List<ChatMessage> history, String systemPrompt,
                                   byte[] fileBytes, String mimeType) {
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("chatWithDocument: fileBytes is empty — falling back to text-only");
            return chat(history, systemPrompt);
        }
        if (fileBytes.length > 5 * 1024 * 1024) {
            log.warn("chatWithDocument: file exceeds 5 MB limit ({} bytes) — aborting multimodal call",
                    fileBytes.length);
            return null;
        }

        log.info("chatWithDocument: sending multimodal request model={} mimeType={} fileSize={} historySize={}",
                model, mimeType, fileBytes.length, history.size());

        List<Map<String, Object>> messages = buildMultimodalMessages(history, fileBytes, mimeType);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", messages
        );

        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            long latencyMs = System.currentTimeMillis() - start;

            if (response == null || !response.has("content")) {
                log.error("chatWithDocument: Claude returned null or missing content");
                errorCounter.increment();
                RequestMetrics.record(model, 0, 0, latencyMs, true);
                return null;
            }

            String stopReason = response.path("stop_reason").asText(null);
            if ("max_tokens".equals(stopReason)) {
                log.warn("chatWithDocument: Claude response truncated by max_tokens={}", maxTokens);
            }

            int inputTokens = response.path("usage").path("input_tokens").asInt(0);
            int outputTokens = response.path("usage").path("output_tokens").asInt(0);
            aiCallTimer.record(latencyMs, TimeUnit.MILLISECONDS);
            inputTokenCounter.increment(inputTokens);
            outputTokenCounter.increment(outputTokens);
            RequestMetrics.record(model, inputTokens, outputTokens, latencyMs, false);

            JsonNode contentArr = response.get("content");
            if (!contentArr.isArray() || contentArr.isEmpty()) return null;

            String text = contentArr.get(0).path("text").asText(null);
            log.info("chatWithDocument: Claude response stopReason={} length={} inputTokens={} outputTokens={} latencyMs={}",
                    stopReason, text == null ? 0 : text.length(), inputTokens, outputTokens, latencyMs);
            return text;

        } catch (WebClientResponseException e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("chatWithDocument: Claude API error status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("chatWithDocument: Claude API call failed: {}", e.getMessage());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        }
    }

    /**
     * Builds a Claude messages array where all prior turns are plain text and the last user turn
     * carries the file attachment as a multimodal content block.
     */
    private List<Map<String, Object>> buildMultimodalMessages(List<ChatMessage> history,
                                                               byte[] fileBytes, String mimeType) {
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        List<Map<String, Object>> messages = new ArrayList<>();

        for (int i = 0; i < history.size() - 1; i++) {
            ChatMessage m = history.get(i);
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        // Last message is the user turn — attach the document
        ChatMessage lastMsg = history.get(history.size() - 1);
        List<Map<String, Object>> contentParts = new ArrayList<>();

        if (mimeType != null && mimeType.startsWith("image/")) {
            contentParts.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", mimeType,
                            "data", base64Data
                    )
            ));
        } else if ("application/pdf".equals(mimeType)) {
            contentParts.add(Map.of(
                    "type", "document",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", "application/pdf",
                            "data", base64Data
                    )
            ));
        } else {
            log.warn("chatWithDocument: unsupported mimeType={} — sending text-only for last turn", mimeType);
        }

        String userText = lastMsg.content() != null ? lastMsg.content() : "";
        contentParts.add(Map.of("type", "text", "text", userText));

        messages.add(Map.of("role", lastMsg.role(), "content", contentParts));
        return messages;
    }

    @Override
    public String providerName() {
        return "claude:" + model;
    }
}
