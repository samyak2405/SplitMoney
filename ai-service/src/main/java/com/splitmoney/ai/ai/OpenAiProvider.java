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
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiChatProvider {

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;
    private final Timer aiCallTimer;
    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;
    private final Counter errorCounter;

    public OpenAiProvider(
            @Value("${ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${ai.openai.api-key}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.openai.max-tokens:1500}") int maxTokens,
            MeterRegistry meterRegistry
    ) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
        this.aiCallTimer = Timer.builder("splity.ai.call.duration.seconds")
                .tag("provider", "openai")
                .description("OpenAI API call duration")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        this.inputTokenCounter = Counter.builder("splity.ai.tokens.total")
                .tag("provider", "openai").tag("direction", "input")
                .description("OpenAI prompt tokens consumed")
                .register(meterRegistry);
        this.outputTokenCounter = Counter.builder("splity.ai.tokens.total")
                .tag("provider", "openai").tag("direction", "output")
                .description("OpenAI completion tokens produced")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("splity.ai.errors.total")
                .tag("provider", "openai")
                .description("OpenAI API errors")
                .register(meterRegistry);
    }

    @Override
    public String chat(List<ChatMessage> history, String systemPrompt) {
        // OpenAI uses a messages array where the system prompt is a role=system message
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", messages,
                "response_format", Map.of("type", "json_object")
        );

        log.info("Calling OpenAI model={} messageCount={}", model, messages.size());
        long start = System.currentTimeMillis();

        try {
            JsonNode response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            long latencyMs = System.currentTimeMillis() - start;

            if (response == null || !response.has("choices")) {
                log.error("OpenAI returned null or missing choices. Response: {}", response);
                errorCounter.increment();
                RequestMetrics.record(model, 0, 0, latencyMs, true);
                return null;
            }

            JsonNode choices = response.get("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;

            String finishReason = choices.get(0).path("finish_reason").asText(null);
            if ("length".equals(finishReason)) {
                log.warn("OpenAI response truncated by max_tokens={} — increase ai.openai.max-tokens", maxTokens);
            }

            int inputTokens = response.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = response.path("usage").path("completion_tokens").asInt(0);
            aiCallTimer.record(latencyMs, TimeUnit.MILLISECONDS);
            inputTokenCounter.increment(inputTokens);
            outputTokenCounter.increment(outputTokens);
            RequestMetrics.record(model, inputTokens, outputTokens, latencyMs, false);

            String text = choices.get(0).path("message").path("content").asText(null);
            log.info("OpenAI response finishReason={} length={} inputTokens={} outputTokens={} latencyMs={}",
                    finishReason, text == null ? 0 : text.length(), inputTokens, outputTokens, latencyMs);
            return text;

        } catch (WebClientResponseException e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("OpenAI API error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("OpenAI API call failed: {}", e.getMessage());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        }
    }

    @Override
    public String chatWithDocument(List<ChatMessage> history, String systemPrompt,
                                   byte[] fileBytes, String mimeType) {
        if ("application/pdf".equals(mimeType)) {
            log.warn("chatWithDocument: OpenAI provider does not support PDF — mimeType={}", mimeType);
            return "{\"action\":\"ASK\",\"reply\":\"PDF processing is only supported with the Claude provider. Please upload a JPEG or PNG image of your bill instead.\"}";
        }

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

        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64Data;

        // Build messages: system prompt as role=system, prior history as plain text, last user turn as multimodal
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

        for (int i = 0; i < history.size() - 1; i++) {
            ChatMessage m = history.get(i);
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }

        // Last user turn with image attachment
        ChatMessage lastMsg = history.get(history.size() - 1);
        String userText = lastMsg.content() != null ? lastMsg.content() : "";
        List<Map<String, Object>> contentParts = List.of(
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)),
                Map.of("type", "text", "text", userText)
        );
        messages.add(Map.of("role", lastMsg.role(), "content", contentParts));

        // NOTE: response_format json_object is incompatible with vision requests in some models
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "messages", messages
        );

        long start = System.currentTimeMillis();
        try {
            JsonNode response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            long latencyMs = System.currentTimeMillis() - start;

            if (response == null || !response.has("choices")) {
                log.error("chatWithDocument: OpenAI returned null or missing choices");
                errorCounter.increment();
                RequestMetrics.record(model, 0, 0, latencyMs, true);
                return null;
            }

            JsonNode choices = response.get("choices");
            if (!choices.isArray() || choices.isEmpty()) return null;

            String finishReason = choices.get(0).path("finish_reason").asText(null);
            if ("length".equals(finishReason)) {
                log.warn("chatWithDocument: OpenAI response truncated by max_tokens={}", maxTokens);
            }

            int inputTokens = response.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = response.path("usage").path("completion_tokens").asInt(0);
            aiCallTimer.record(latencyMs, TimeUnit.MILLISECONDS);
            inputTokenCounter.increment(inputTokens);
            outputTokenCounter.increment(outputTokens);
            RequestMetrics.record(model, inputTokens, outputTokens, latencyMs, false);

            String text = choices.get(0).path("message").path("content").asText(null);
            log.info("chatWithDocument: OpenAI response finishReason={} length={} inputTokens={} outputTokens={} latencyMs={}",
                    finishReason, text == null ? 0 : text.length(), inputTokens, outputTokens, latencyMs);
            return text;

        } catch (WebClientResponseException e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("chatWithDocument: OpenAI API error status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.error("chatWithDocument: OpenAI API call failed: {}", e.getMessage());
            errorCounter.increment();
            RequestMetrics.record(model, 0, 0, latencyMs, true);
            return null;
        }
    }

    @Override
    public String providerName() {
        return "openai:" + model;
    }
}
