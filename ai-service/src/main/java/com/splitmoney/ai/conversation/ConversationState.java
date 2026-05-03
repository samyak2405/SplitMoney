package com.splitmoney.ai.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState {

    public enum Stage {
        AWAITING_PARTICIPANTS,
        AWAITING_SPLIT_TYPE,
        AWAITING_EXACT_AMOUNTS,
        AWAITING_PERCENTAGES,
        AWAITING_CONFIRMATION,
        COMPLETED,
        CANCELLED
    }

    private String groupId;
    private String userId;
    private String userEmail;
    private String userJwt;

    // Bill data being collected
    private String description;
    private String totalAmount;
    private String currency;
    private String paidByEmail;

    // Group context (fetched once at conversation start)
    private String groupName;
    private List<String> groupMemberEmails;
    private String groupCurrency;

    // Conversation progression
    private Stage stage;
    private List<String> participantEmails;
    private String splitType;
    private Map<String, String> exactAmounts;
    private Map<String, String> percentages;

    private long createdAtEpochMs;
    private long updatedAtEpochMs;
    private int claudeTurns;

    // Generated once per expense flow (at READY/AWAITING_CONFIRMATION) to guarantee idempotency key uniqueness
    private String expenseIdempotencyKey;

    public record ChatMessage(String role, String content) {}

    public Map<String, String> getExactAmounts() {
        if (exactAmounts == null) exactAmounts = new HashMap<>();
        return exactAmounts;
    }

    public Map<String, String> getPercentages() {
        if (percentages == null) percentages = new HashMap<>();
        return percentages;
    }
}
