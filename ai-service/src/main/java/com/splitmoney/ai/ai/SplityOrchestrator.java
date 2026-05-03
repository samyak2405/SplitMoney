package com.splitmoney.ai.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.ai.conversation.ConversationState;
import com.splitmoney.ai.conversation.ConversationState.Stage;
import com.splitmoney.ai.splitwise.SplitwiseClient;
import com.splitmoney.ai.splitwise.dto.UserBalancesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SplityOrchestrator {

    private static final Set<String> YES_WORDS = Set.of(
            "yes", "yeah", "yep", "sure", "ok", "okay", "confirm", "create",
            "go ahead", "do it", "proceed", "done", "looks good", "correct", "right");
    private static final Set<String> NO_WORDS = Set.of(
            "no", "nope", "cancel", "stop", "abort", "nevermind", "never mind");

    // Keep only recent turns — old expense flows confuse the model
    private static final int MAX_HISTORY = 10;

    private final AiChatProvider aiChatProvider;
    private final ObjectMapper objectMapper;
    private final SplitwiseClient splitwiseClient;

    /**
     * Like {@link #process} but sends the attached bill/receipt to the AI provider as a multimodal
     * message. The system prompt is augmented with bill-parsing instructions.
     */
    public String processWithDocument(String userMessage, byte[] fileBytes, String mimeType,
                                      String documentName, ConversationState state,
                                      List<ConversationState.ChatMessage> history) {
        log.info("processWithDocument stage={} mimeType={} fileSize={} documentName='{}'",
                state.getStage(), mimeType, fileBytes.length, documentName);

        // Allow cancellation mid-flow but skip the AWAITING_CONFIRMATION shortcut
        // (a new attachment always starts/extends a collection flow, not a confirmation)
        if (state.getStage() != null
                && state.getStage() != ConversationState.Stage.AWAITING_CONFIRMATION
                && NO_WORDS.stream().anyMatch(userMessage.trim().toLowerCase()::contains)) {
            log.info("processWithDocument: NO_WORDS cancel shortcut at stage={}", state.getStage());
            state.setStage(Stage.CANCELLED);
            return "Alright, I've cancelled that. Feel free to start again anytime!";
        }

        List<ConversationState.ChatMessage> fullHistory = sanitizeHistory(history);
        // Use placeholder text if user sent no text alongside the attachment
        String effectiveMessage = (userMessage != null && !userMessage.isBlank())
                ? userMessage : "Please parse this bill and help me create an expense.";
        fullHistory.add(new ConversationState.ChatMessage("user", effectiveMessage));

        String systemPrompt = buildSystemPromptWithDocument(state, documentName);
        log.info("processWithDocument: calling AI with sanitizedHistory={} messages", fullHistory.size());

        String rawResponse = aiChatProvider.chatWithDocument(fullHistory, systemPrompt, fileBytes, mimeType);
        log.info("processWithDocument: AI raw response length={}",
                rawResponse != null ? rawResponse.length() : 0);

        if (rawResponse == null) {
            log.warn("processWithDocument: AI returned null — mimeType={} fileSize={}", mimeType, fileBytes.length);
            return "I couldn't read the attached file. Please ensure it's under 5 MB and try again.";
        }

        BillExtractionResult result = parseAction(rawResponse);
        if (result == null) {
            log.warn("processWithDocument: parseAction returned null — returning rawResponse");
            return rawResponse;
        }

        log.info("processWithDocument: parsed action={} stage={}", result.getAction(), state.getStage());
        state.setClaudeTurns(state.getClaudeTurns() + 1);
        String action = result.getAction() != null ? result.getAction().toUpperCase() : "";

        return switch (action) {
            case "READY" -> handleReady(result, state, rawResponse);
            case "QUERY" -> handleQuery(result, effectiveMessage, state, fullHistory);
            case "TEXT" -> result.getReply() != null ? result.getReply() : rawResponse;
            case "CANCEL" -> {
                state.setStage(Stage.CANCELLED);
                yield result.getReply() != null ? result.getReply() : "Cancelled.";
            }
            case "ASK", "CLARIFY" -> {
                updateStageFromReply(result.getReply() != null ? result.getReply() : rawResponse, state);
                yield result.getReply() != null ? result.getReply() : rawResponse;
            }
            default -> {
                log.warn("processWithDocument: unknown action '{}' from AI", result.getAction());
                yield result.getReply() != null ? result.getReply() : rawResponse;
            }
        };
    }

    public String process(String userMessage, ConversationState state, List<ConversationState.ChatMessage> history) {
        log.info("Orchestrator.process stage={} historySize={} msg='{}'",
                state.getStage(), history.size(), abbreviate(userMessage, 80));

        // Confirmation shortcut — bypass AI for yes/no at confirmation stage
        if (state.getStage() == Stage.AWAITING_CONFIRMATION) {
            String lower = userMessage.trim().toLowerCase();
            if (YES_WORDS.stream().anyMatch(lower::contains)) {
                log.info("YES_WORDS matched at AWAITING_CONFIRMATION — setting COMPLETED");
                state.setStage(Stage.COMPLETED);
                return null;
            }
            if (NO_WORDS.stream().anyMatch(lower::contains)) {
                log.info("NO_WORDS matched at AWAITING_CONFIRMATION — setting CANCELLED");
                state.setStage(Stage.CANCELLED);
                return "Got it, cancelled! Let me know if you need anything else.";
            }
            log.info("AWAITING_CONFIRMATION but no YES/NO match — falling through to AI");
        }

        // Cancel shortcut only when explicitly cancelling an active expense flow
        if (state.getStage() != null
                && state.getStage() != Stage.AWAITING_CONFIRMATION
                && NO_WORDS.stream().anyMatch(userMessage.trim().toLowerCase()::contains)) {
            log.info("NO_WORDS cancel shortcut fired at stage={}", state.getStage());
            state.setStage(Stage.CANCELLED);
            return "Alright, I've cancelled that. Feel free to start again anytime!";
        }

        List<ConversationState.ChatMessage> fullHistory = sanitizeHistory(history);
        fullHistory.add(new ConversationState.ChatMessage("user", userMessage));
        log.info("Calling AI with sanitizedHistory={} messages", fullHistory.size());

        String rawResponse = aiChatProvider.chat(fullHistory, buildSystemPrompt(state));

        if (rawResponse == null) {
            log.warn("AI returned null response");
            return "Sorry, I'm having trouble right now. Please try again in a moment.";
        }

        BillExtractionResult result = parseAction(rawResponse);
        if (result == null) {
            log.warn("parseAction returned null — returning rawResponse as-is");
            return rawResponse;
        }

        log.info("Parsed action={} queryType={} extracted={}",
                result.getAction(), result.getQueryType(),
                result.getExtracted() != null ? "present" : "null");

        state.setClaudeTurns(state.getClaudeTurns() + 1);
        String action = result.getAction() != null ? result.getAction().toUpperCase() : "";

        return switch (action) {
            case "READY" -> handleReady(result, state, rawResponse);
            case "QUERY" -> handleQuery(result, userMessage, state, fullHistory);
            case "TEXT" -> result.getReply() != null ? result.getReply() : rawResponse;
            case "CANCEL" -> {
                state.setStage(Stage.CANCELLED);
                yield result.getReply() != null ? result.getReply() : "Cancelled.";
            }
            case "ASK", "CLARIFY" -> {
                updateStageFromReply(result.getReply() != null ? result.getReply() : rawResponse, state);
                yield result.getReply() != null ? result.getReply() : rawResponse;
            }
            default -> {
                log.warn("Unknown action '{}' from AI", result.getAction());
                yield result.getReply() != null ? result.getReply() : rawResponse;
            }
        };
    }

    // ── Action handlers ──────────────────────────────────────────────────────

    private String handleReady(BillExtractionResult result, ConversationState state, String rawResponse) {
        BillExtractionResult.Extracted ex = result.getExtracted();
        if (ex == null) {
            log.warn("READY action but extracted is null — treating as ASK");
            updateStageFromReply(result.getReply() != null ? result.getReply() : rawResponse, state);
            return result.getReply() != null ? result.getReply() : rawResponse;
        }
        String validationError = validateExtracted(ex, state.getGroupMemberEmails());
        if (validationError != null) {
            log.warn("READY validation failed: {}", validationError);
            return "Hmm, " + validationError + " Could you clarify?";
        }
        applyExtracted(ex, state);
        state.setStage(Stage.AWAITING_CONFIRMATION);
        log.info("READY validated — stage=AWAITING_CONFIRMATION description='{}' amount={} paidBy={}",
                state.getDescription(), state.getTotalAmount(), state.getPaidByEmail());
        return buildConfirmationSummary(state);
    }

    private String handleQuery(BillExtractionResult result, String userMessage,
                               ConversationState state, List<ConversationState.ChatMessage> historyWithUser) {
        String queryType = result.getQueryType();
        log.info("QUERY action queryType={}", queryType);

        String dataContext = fetchDataForQuery(queryType, state);

        if (dataContext == null) {
            return "Sorry, I couldn't retrieve that data right now. Please try again in a moment.";
        }

        // Second-pass: call AI with the fetched data to generate the actual answer
        String enrichedPrompt = buildDataAnswerPrompt(state, dataContext);
        List<ConversationState.ChatMessage> secondPassHistory = new ArrayList<>(historyWithUser);
        // Inject data as a system-like context message
        secondPassHistory.add(new ConversationState.ChatMessage("assistant",
                "{\"action\":\"QUERY\",\"reply\":\"" + (result.getReply() != null ? result.getReply() : "Let me check that for you…") + "\"}"));
        secondPassHistory.add(new ConversationState.ChatMessage("user",
                "Here is the live data fetched from the backend:\n" + dataContext
                        + "\n\nNow answer the user's original question: " + userMessage));

        String secondRaw = aiChatProvider.chat(secondPassHistory, enrichedPrompt);
        if (secondRaw == null) {
            return "Sorry, I couldn't generate an answer right now. Please try again.";
        }

        BillExtractionResult secondResult = parseAction(secondRaw);
        if (secondResult != null && secondResult.getReply() != null) {
            return secondResult.getReply();
        }
        // If the second pass didn't produce JSON (shouldn't happen), return raw text
        return secondRaw;
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    private String fetchDataForQuery(String queryType, ConversationState state) {
        if ("BALANCES".equalsIgnoreCase(queryType) || "MEMBER_OWED_TO_USER".equalsIgnoreCase(queryType)) {
            if (state.getGroupName() == null) {
                log.warn("Cannot fetch balances — groupName is null in state");
                return null;
            }
            UserBalancesResponse response = splitwiseClient.getUserBalances(state.getGroupName(), state.getUserJwt());
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("getUserBalances returned null/failure for group={}", state.getGroupName());
                return null;
            }
            return formatBalancesAsText(response.getData(), state.getUserEmail());
        }
        // GENERAL — AI answers from its own knowledge, return a non-null empty string so the second pass runs
        return "";
    }

    private String formatBalancesAsText(UserBalancesResponse.Data data, String userEmail) {
        String currency = data.getCurrency() != null ? data.getCurrency() : "";
        StringBuilder sb = new StringBuilder();
        sb.append("Group: ").append(data.getGroupName()).append(" | Currency: ").append(currency).append("\n");
        sb.append("User: ").append(userEmail).append("\n");
        sb.append("Total others owe you: ").append(currency).append(" ").append(data.getTotalToReceive()).append("\n");
        sb.append("Total you owe others: ").append(currency).append(" ").append(data.getTotalToPay()).append("\n");

        if (data.getMembersWhoNeedToPayUser() != null && !data.getMembersWhoNeedToPayUser().isEmpty()) {
            sb.append("Members who owe you:\n");
            data.getMembersWhoNeedToPayUser().forEach(m ->
                    sb.append("  - ").append(m.getEmail()).append(" owes ").append(currency).append(" ").append(m.getAmount()).append("\n"));
        } else {
            sb.append("No one owes you in this group.\n");
        }

        if (data.getMembersUserNeedsToPay() != null && !data.getMembersUserNeedsToPay().isEmpty()) {
            sb.append("Members you owe:\n");
            data.getMembersUserNeedsToPay().forEach(m ->
                    sb.append("  - You owe ").append(m.getEmail()).append(" ").append(currency).append(" ").append(m.getAmount()).append("\n"));
        } else {
            sb.append("You don't owe anyone in this group.\n");
        }
        return sb.toString();
    }

    // ── Prompts ───────────────────────────────────────────────────────────────

    private String buildSystemPrompt(ConversationState state) {
        String members = state.getGroupMemberEmails() != null && !state.getGroupMemberEmails().isEmpty()
                ? String.join(", ", state.getGroupMemberEmails()) : "unknown";
        String currency = state.getGroupCurrency() != null ? state.getGroupCurrency() : "INR";

        String groupName = state.getGroupName() != null ? state.getGroupName() : "this group";
        return """
                You are Splity, the AI assistant for SplitMoney — an expense-splitting app.
                You MUST respond with a single JSON object only — no plain text, no markdown, ONLY JSON.

                CURRENT CONTEXT:
                - Group: %s
                - Group members (use these exact emails): [%s]
                - Group default currency: %s
                - Logged-in user: %s

                WHAT YOU CAN DO:
                1. CREATE EXPENSES: Collect bill details and split among the group members above.
                2. ANSWER BALANCE QUESTIONS: Use QUERY to fetch live balance/debt data for this group.
                3. ANSWER GENERAL QUESTIONS: Use TEXT to answer questions about how the app works, split types, etc.

                WHAT YOU CANNOT DO (redirect helpfully, never say "I can't check right now"):
                - You cannot see how many groups the user belongs to overall — tell them to check the "Groups" section in the left sidebar.
                - You cannot see expenses from other groups — tell them to switch to that group.
                - You cannot send money, edit past expenses, or invite members — tell them to use the relevant tab (Expenses, Members, etc.).

                Response formats — output EXACTLY ONE JSON object:

                While collecting expense info:
                {"action":"ASK","reply":"<short friendly question>"}

                When you have ALL expense fields (description, totalAmount, currency, paidByEmail, splitType, participants with real emails):
                {"action":"READY","reply":"<one-line summary>","extracted":{"description":"...","totalAmount":"123","currency":"INR","paidByEmail":"a@x.com","splitType":"EQUAL","participants":[{"email":"a@x.com"},{"email":"b@x.com"}]}}

                For EXACT split:
                {"action":"READY","reply":"<summary>","extracted":{"description":"...","totalAmount":"100","currency":"INR","paidByEmail":"a@x.com","splitType":"EXACT","participants":[{"email":"a@x.com","amount":"60"},{"email":"b@x.com","amount":"40"}]}}

                For PERCENTAGE split:
                {"action":"READY","reply":"<summary>","extracted":{"description":"...","totalAmount":"100","currency":"INR","paidByEmail":"a@x.com","splitType":"PERCENTAGE","participants":[{"email":"a@x.com","percentage":"60"},{"email":"b@x.com","percentage":"40"}]}}

                When the user asks about balances, debts, who owes whom, or how much they owe/are owed IN THIS GROUP:
                {"action":"QUERY","queryType":"BALANCES","reply":"Let me check your balances…"}

                When you can answer directly (app how-tos, split type explanations, redirects for things outside your scope, etc.):
                {"action":"TEXT","reply":"<your helpful answer or redirect>"}

                To cancel an expense flow only:
                {"action":"CANCEL","reply":"Got it, cancelled!"}

                CRITICAL RULES:
                - NEVER use CANCEL for questions. Use QUERY or TEXT for any question.
                - NEVER say "I can't check right now" — always give a helpful answer or redirect.
                - Use QUERY only when you need live balance/debt data for this group. Use TEXT for everything else.
                - Only use READY when you truly have ALL required fields with valid email addresses from the group.
                - Keep "reply" short and conversational.
                - Output ONLY the JSON object. Nothing before or after it.
                """.formatted(groupName, members, currency, state.getUserEmail() != null ? state.getUserEmail() : "user");
    }

    private String buildSystemPromptWithDocument(ConversationState state, String documentName) {
        String base = buildSystemPrompt(state);
        String nameHint = (documentName != null && !documentName.isBlank())
                ? " (file: " + documentName + ")" : "";
        return base + """

                BILL/RECEIPT ATTACHED%s: The user has provided a bill or receipt image/PDF.
                Parse it and extract:
                - description: the merchant or restaurant name
                - totalAmount: the grand total or amount due
                - currency: the currency shown on the bill (default to group currency if not visible)
                Extract ONLY what is clearly visible on the bill. Do NOT guess ambiguous values — use ASK instead.
                After extracting visible data, proceed to collect any missing fields (who paid, how to split).
                """.formatted(nameHint);
    }

    private String buildDataAnswerPrompt(ConversationState state, String dataContext) {
        return """
                You are Splity, the AI assistant for SplitMoney.
                You MUST respond with a single JSON object only.

                You have been given live data from the backend. Answer the user's question based on this data.
                Be concise, friendly, and use the currency symbol where appropriate.

                Response format:
                {"action":"TEXT","reply":"<your clear, friendly answer>"}

                Output ONLY the JSON object. Nothing else.
                """;
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private BillExtractionResult parseAction(String rawResponse) {
        int actionStart = rawResponse.lastIndexOf("{\"action\":");
        if (actionStart < 0) {
            log.warn("parseAction: no {{\"action\": found in: {}", abbreviate(rawResponse, 200));
            return null;
        }
        String jsonPart = rawResponse.substring(actionStart).trim();
        int closingBrace = jsonPart.lastIndexOf('}');
        if (closingBrace < 0) {
            log.warn("parseAction: no closing brace in: {}", abbreviate(jsonPart, 200));
            return null;
        }
        jsonPart = jsonPart.substring(0, closingBrace + 1);
        try {
            return objectMapper.readValue(jsonPart, BillExtractionResult.class);
        } catch (Exception e) {
            log.warn("parseAction: JSON parse failed: {} — snippet: {}", e.getMessage(), abbreviate(jsonPart, 300));
            return null;
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private String validateExtracted(BillExtractionResult.Extracted ex, List<String> groupMembers) {
        if (ex.getDescription() == null || ex.getDescription().isBlank())
            return "I need a description for the expense.";
        if (ex.getTotalAmount() == null || ex.getTotalAmount().isBlank())
            return "I need the total amount.";
        if (ex.getPaidByEmail() == null || ex.getPaidByEmail().isBlank())
            return "Who paid for this?";
        if (ex.getSplitType() == null)
            return "I need the split type (equal/exact/percentage).";
        if (ex.getParticipants() == null || ex.getParticipants().isEmpty())
            return "I need at least one participant.";

        Set<String> memberSet = groupMembers != null
                ? groupMembers.stream().map(String::toLowerCase).collect(Collectors.toSet())
                : Set.of();
        for (BillExtractionResult.Participant p : ex.getParticipants()) {
            if (p.getEmail() == null) continue;
            if (!memberSet.isEmpty() && !memberSet.contains(p.getEmail().toLowerCase()))
                return p.getEmail() + " is not a member of this group.";
        }

        if ("EXACT".equalsIgnoreCase(ex.getSplitType())) {
            try {
                BigDecimal total = new BigDecimal(ex.getTotalAmount());
                BigDecimal sum = ex.getParticipants().stream()
                        .filter(p -> p.getAmount() != null)
                        .map(p -> new BigDecimal(p.getAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.subtract(sum).abs().compareTo(new BigDecimal("0.02")) > 0)
                    return "the amounts don't add up to " + ex.getTotalAmount() + " (they sum to " + sum + ").";
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // ── State helpers ─────────────────────────────────────────────────────────

    private void applyExtracted(BillExtractionResult.Extracted ex, ConversationState state) {
        if (state.getExpenseIdempotencyKey() == null) {
            state.setExpenseIdempotencyKey("splity-" + UUID.randomUUID());
        }
        state.setDescription(ex.getDescription());
        state.setTotalAmount(ex.getTotalAmount());
        if (ex.getCurrency() != null) state.setCurrency(ex.getCurrency());
        else if (state.getGroupCurrency() != null) state.setCurrency(state.getGroupCurrency());
        state.setPaidByEmail(ex.getPaidByEmail());
        state.setSplitType(ex.getSplitType().toUpperCase());

        List<String> emails = new ArrayList<>();
        for (BillExtractionResult.Participant p : ex.getParticipants()) {
            if (p.getEmail() != null) emails.add(p.getEmail());
            if ("EXACT".equalsIgnoreCase(ex.getSplitType()) && p.getAmount() != null)
                state.getExactAmounts().put(p.getEmail(), p.getAmount());
            if ("PERCENTAGE".equalsIgnoreCase(ex.getSplitType()) && p.getPercentage() != null)
                state.getPercentages().put(p.getEmail(), p.getPercentage());
        }
        state.setParticipantEmails(emails);
    }

    private String buildConfirmationSummary(ConversationState state) {
        StringBuilder sb = new StringBuilder("Here's the expense summary:\n");
        sb.append("📋 ").append(state.getDescription()).append("\n");
        sb.append("💰 ").append(state.getCurrency()).append(" ").append(state.getTotalAmount())
                .append(" paid by ").append(state.getPaidByEmail()).append("\n");
        sb.append("👥 Split ").append(state.getSplitType().toLowerCase()).append(" among: ");
        sb.append(String.join(", ", state.getParticipantEmails())).append("\n");

        if ("EXACT".equalsIgnoreCase(state.getSplitType())) {
            state.getExactAmounts().forEach((email, amt) ->
                    sb.append("  • ").append(email).append(" → ").append(state.getCurrency()).append(" ").append(amt).append("\n"));
        } else if ("PERCENTAGE".equalsIgnoreCase(state.getSplitType())) {
            state.getPercentages().forEach((email, pct) ->
                    sb.append("  • ").append(email).append(" → ").append(pct).append("%\n"));
        }
        sb.append("\nShall I create this expense? Reply *yes* to confirm or *cancel* to abort.");
        return sb.toString();
    }

    private void updateStageFromReply(String reply, ConversationState state) {
        if (state.getStage() != null) return;
        String lower = reply.toLowerCase();
        if (lower.contains("who") && (lower.contains("include") || lower.contains("split with")))
            state.setStage(Stage.AWAITING_PARTICIPANTS);
        else if (lower.contains("equal") || lower.contains("exact") || lower.contains("percentage") || lower.contains("split type"))
            state.setStage(Stage.AWAITING_SPLIT_TYPE);
        else if (lower.contains("amount") || lower.contains("share"))
            state.setStage(Stage.AWAITING_EXACT_AMOUNTS);
        else if (lower.contains("percent"))
            state.setStage(Stage.AWAITING_PERCENTAGES);
    }

    private List<ConversationState.ChatMessage> sanitizeHistory(List<ConversationState.ChatMessage> history) {
        List<ConversationState.ChatMessage> trimmed = history.size() > MAX_HISTORY
                ? new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size()))
                : new ArrayList<>(history);

        if (history.size() > MAX_HISTORY) {
            log.info("sanitizeHistory: trimmed from {} to {}", history.size(), trimmed.size());
        }

        List<ConversationState.ChatMessage> sanitized = new ArrayList<>();
        for (ConversationState.ChatMessage msg : trimmed) {
            if (!sanitized.isEmpty() && sanitized.get(sanitized.size() - 1).role().equals(msg.role())) {
                sanitized.set(sanitized.size() - 1, msg);
            } else {
                sanitized.add(msg);
            }
        }
        if (!sanitized.isEmpty() && !"user".equals(sanitized.get(0).role())) {
            sanitized.remove(0);
        }
        return sanitized;
    }

    private String abbreviate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
