package com.splitmoney.ai.controller;

import com.splitmoney.ai.ai.SplityOrchestrator;
import com.splitmoney.ai.cassandra.SplityMessage;
import com.splitmoney.ai.cassandra.SplityMessageService;
import com.splitmoney.ai.conversation.ConversationState;
import com.splitmoney.ai.conversation.ConversationState.ChatMessage;
import com.splitmoney.ai.conversation.ConversationState.Stage;
import com.splitmoney.ai.conversation.ConversationStateService;
import com.splitmoney.ai.document.DocumentServiceClient;
import com.splitmoney.ai.model.AiInteractionLog;
import com.splitmoney.ai.observability.ChatObservationData;
import com.splitmoney.ai.observability.ObservabilityService;
import com.splitmoney.ai.observability.RequestMetrics;
import com.splitmoney.ai.repository.AiInteractionLogRepository;
import com.splitmoney.ai.security.RequestJwtExtractor;
import com.splitmoney.ai.security.RequestJwtExtractor.UserPrincipal;
import com.splitmoney.ai.splitwise.SplitwiseClient;
import com.splitmoney.ai.splitwise.dto.GroupMembersResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/ai/v1/groups/{groupId}/splity")
@RequiredArgsConstructor
public class SplityController {

    private final RequestJwtExtractor jwtExtractor;
    private final SplityMessageService messageService;
    private final ConversationStateService stateService;
    private final SplityOrchestrator orchestrator;
    private final SplitwiseClient splitwiseClient;
    private final AiInteractionLogRepository logRepository;
    private final DocumentServiceClient documentServiceClient;
    private final ObservabilityService observabilityService;

    // ── GET history ──────────────────────────────────────────────────────────────

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getHistory(
            @PathVariable String groupId,
            HttpServletRequest request
    ) {
        UserPrincipal user = jwtExtractor.extract(request);
        List<SplityMessage> history = messageService.loadRawHistory(groupId, user.userId().toString(), 100);

        List<Map<String, Object>> items = history.stream()
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("messageId", m.getMessageId().toString());
                    item.put("role", m.getRole());
                    item.put("content", m.getContent());
                    item.put("createdAtEpochMs", m.getCreatedAt().toEpochMilli());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(Map.of("success", true, "data", items));
    }

    // ── POST chat ─────────────────────────────────────────────────────────────────

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @PathVariable String groupId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        String traceId = UUID.randomUUID().toString();
        long requestStart = System.currentTimeMillis();
        RequestMetrics.clear(); // ensure clean ThreadLocal for this request

        String content = body.getOrDefault("content", "").trim();
        String documentId       = body.get("documentId");
        String documentMimeType = body.get("documentMimeType");
        String documentName     = body.get("documentName");
        String documentBase64   = body.get("documentBase64"); // embedded file bytes (preferred path)

        // Require at least a text message OR a document attachment
        if (content.isBlank() && (documentId == null || documentId.isBlank())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "content or a document attachment is required"));
        }

        UserPrincipal user = jwtExtractor.extract(request);
        String userId = user.userId().toString();
        String userJwt = jwtExtractor.resolveToken(request);

        // 1. Save user message to Cassandra (prefix with attachment name so history reflects it)
        String storedContent = (documentName != null && !documentName.isBlank())
                ? (content.isBlank() ? "[Bill: " + documentName + "]"
                                     : "[Bill: " + documentName + "]\n" + content)
                : content;
        messageService.saveMessage(groupId, userId, "user", storedContent);

        // 2. Load conversation history from Cassandra (AI memory)
        List<ChatMessage> history = messageService.loadHistoryForAi(groupId, userId);
        // The user message we just saved is the last in history — pass history excluding it so orchestrator appends it cleanly
        List<ChatMessage> historyWithoutLast = history.isEmpty() ? history : history.subList(0, history.size() - 1);

        // 3. Load or create conversation state from Redis
        ConversationState state = stateService.load(groupId, userId)
                .orElseGet(() -> {
                    log.info("No Redis state found — starting fresh for groupId={} userId={}", groupId, userId);
                    return ConversationState.builder()
                            .groupId(groupId)
                            .userId(userId)
                            .userEmail(user.email())
                            .userJwt(userJwt)
                            .createdAtEpochMs(System.currentTimeMillis())
                            .build();
                });
        log.info("Loaded state groupId={} userId={} stage={}", groupId, userId, state.getStage());

        // Guard: if a stale terminal state was somehow not deleted, start fresh
        if (state.getStage() == Stage.COMPLETED || state.getStage() == Stage.CANCELLED) {
            log.warn("Stale terminal state found for groupId={} userId={} stage={} — resetting",
                    groupId, userId, state.getStage());
            stateService.delete(groupId, userId);
            state = ConversationState.builder()
                    .groupId(groupId)
                    .userId(userId)
                    .userEmail(user.email())
                    .userJwt(userJwt)
                    .createdAtEpochMs(System.currentTimeMillis())
                    .build();
        }
        state.setUserJwt(userJwt);

        // 4. Fetch group members once at conversation start
        if (state.getGroupMemberEmails() == null || state.getGroupMemberEmails().isEmpty()) {
            GroupMembersResponse members = splitwiseClient.getGroupMembers(Long.parseLong(groupId), userJwt);
            if (members != null && members.getData() != null) {
                state.setGroupMemberEmails(
                        members.getData().getMembers().stream()
                                .map(GroupMembersResponse.Member::getEmail)
                                .toList()
                );
                state.setGroupCurrency(members.getData().getCurrency());
                state.setGroupName(members.getData().getGroupName());
            }
        }
        state.setUpdatedAtEpochMs(System.currentTimeMillis());

        // 5. Resolve document bytes for AI processing
        // Primary path: use base64 sent directly by the frontend (file is already in browser memory)
        // Fallback: fetch from document-service via service-to-service call
        byte[] fileBytes = null;
        if (documentBase64 != null && !documentBase64.isBlank()) {
            try {
                fileBytes = Base64.getDecoder().decode(documentBase64);
                log.info("Using embedded document base64 documentName='{}' mimeType={} size={} bytes",
                        documentName, documentMimeType, fileBytes.length);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to decode documentBase64 for documentName='{}': {}", documentName, e.getMessage());
            }
        } else if (documentId != null && !documentId.isBlank()) {
            log.info("No base64 provided — fetching document from document-service documentId={} mimeType={} name='{}'",
                    documentId, documentMimeType, documentName);
            try {
                fileBytes = documentServiceClient.fetchDocumentBytes(documentId, userJwt);
                log.info("Document fetched from service documentId={} size={} bytes",
                        documentId, fileBytes != null ? fileBytes.length : 0);
            } catch (Exception e) {
                log.warn("Could not fetch document {} from document-service — proceeding text-only: {}",
                        documentId, e.getMessage());
            }
        }

        // 6. Call orchestrator — route to multimodal path when a document was successfully fetched
        String replyText;
        try {
            if (fileBytes != null && documentMimeType != null) {
                log.info("Routing to processWithDocument groupId={} userId={} mimeType={}",
                        groupId, userId, documentMimeType);
                replyText = orchestrator.processWithDocument(
                        content, fileBytes, documentMimeType, documentName, state, historyWithoutLast);
            } else {
                if (documentId != null && !documentId.isBlank()) {
                    log.warn("documentId={} present but fileBytes=null — falling back to text-only", documentId);
                }
                replyText = orchestrator.process(content, state, historyWithoutLast);
            }
        } catch (Exception e) {
            log.error("Orchestrator error groupId={} userId={}", groupId, userId, e);
            replyText = "Sorry, something went wrong. Please try again in a moment.";
        }
        log.info("Post-orchestrator stage groupId={} userId={} stage={}", groupId, userId, state.getStage());

        // 6. Handle completion
        boolean expenseCreatedSuccessfully = false;
        if (state.getStage() == Stage.COMPLETED) {
            log.info("Stage COMPLETED — calling createExpense groupId={} userId={} splitType={} totalAmount={} paidBy={}",
                    groupId, userId, state.getSplitType(), state.getTotalAmount(), state.getPaidByEmail());
            Long expenseId = null;
            String createError = null;
            try {
                expenseId = splitwiseClient.createExpense(state, userJwt);
                log.info("createExpense returned expenseId={} groupId={} userId={}", expenseId, groupId, userId);
            } catch (Exception e) {
                log.error("Expense creation failed groupId={}", groupId, e);
                createError = e.getMessage();
            }
            persistLog(state, user, content, expenseId, createError);
            stateService.delete(groupId, userId);
            if (expenseId != null) {
                expenseCreatedSuccessfully = true;
                replyText = "Expense created successfully! 🎉";
            } else {
                replyText = "I had trouble creating the expense. Please try adding it manually.";
            }
        } else if (state.getStage() == Stage.CANCELLED) {
            persistLog(state, user, content, null, null);
            stateService.delete(groupId, userId);
        } else {
            stateService.save(state);
        }

        // 7. Save AI reply to Cassandra
        if (replyText == null) replyText = "Done!";
        SplityMessage aiMsg = messageService.saveMessage(groupId, userId, "assistant", replyText);

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("messageId", aiMsg.getMessageId().toString());
        responseData.put("role", "assistant");
        responseData.put("content", replyText);
        responseData.put("createdAtEpochMs", aiMsg.getCreatedAt().toEpochMilli());
        responseData.put("expenseCreated", expenseCreatedSuccessfully);

        // Observability: record Prometheus metrics + ship Langfuse trace (async)
        long requestLatencyMs = System.currentTimeMillis() - requestStart;
        RequestMetrics.Snapshot aiMetrics = RequestMetrics.getAndClear();
        boolean wasCancelled = state.getStage() == Stage.CANCELLED;
        boolean hasDocument = (documentMimeType != null && !documentMimeType.isBlank());
        ChatObservationData observationData = new ChatObservationData(
                traceId, userId, user.email(), groupId,
                storedContent,
                replyText, expenseCreatedSuccessfully, wasCancelled,
                hasDocument, documentMimeType,
                aiMetrics, requestLatencyMs, state.getClaudeTurns()
        );
        observabilityService.recordMetrics(observationData);
        observabilityService.sendToLangfuse(observationData);

        return ResponseEntity.ok(Map.of("success", true, "data", responseData));
    }

    private void persistLog(ConversationState state, UserPrincipal user,
                            String triggerMessage, Long expenseId, String error) {
        try {
            AiInteractionLog log = new AiInteractionLog();
            log.setGroupId(state.getGroupId());
            log.setUserId(user.userId());
            log.setUserEmail(user.email());
            log.setTriggerMessage(triggerMessage);
            log.setStageReached(state.getStage() != null ? state.getStage().name() : "UNKNOWN");
            log.setExpenseCreated(expenseId != null);
            log.setExpenseId(expenseId);
            if (state.getTotalAmount() != null) {
                try { log.setTotalAmount(new BigDecimal(state.getTotalAmount())); } catch (Exception ignored) {}
            }
            log.setCurrency(state.getCurrency());
            log.setDescription(state.getDescription());
            log.setPaidByEmail(state.getPaidByEmail());
            log.setSplitType(state.getSplitType());
            log.setClaudeTurns(state.getClaudeTurns());
            log.setErrorMessage(error);
            log.setCompletedAt(Instant.now());
            logRepository.save(log);
        } catch (Exception e) {
            SplityController.log.warn("Failed to persist AI interaction log: {}", e.getMessage());
        }
    }
}
