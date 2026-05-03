package com.splitmoney.ai.splitwise;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmoney.ai.conversation.ConversationState;
import com.splitmoney.ai.splitwise.dto.CreateExpenseRequest;
import com.splitmoney.ai.splitwise.dto.GroupMembersResponse;
import com.splitmoney.ai.splitwise.dto.UserBalancesResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SplitwiseClient {

    private final WebClient webClient;

    public SplitwiseClient(@Qualifier("spliwtiseWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public GroupMembersResponse getGroupMembers(Long groupId, String userJwt) {
        try {
            return webClient.get()
                    .uri("/api/splitwise/v1/groups/{groupId}/members", groupId)
                    .header("Authorization", "Bearer " + userJwt)
                    .retrieve()
                    .bodyToMono(GroupMembersResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to fetch group members groupId={} status={}", groupId, e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Error fetching group members groupId={}", groupId, e);
            return null;
        }
    }

    public UserBalancesResponse getUserBalances(String groupName, String userJwt) {
        Map<String, String> body = Map.of(
                "requestId", UUID.randomUUID().toString(),
                "groupName", groupName
        );
        log.info("getUserBalances called groupName={}", groupName);
        try {
            UserBalancesResponse response = webClient.post()
                    .uri("/api/splitwise/v1/user-balances")
                    .header("Authorization", "Bearer " + userJwt)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(UserBalancesResponse.class)
                    .block();
            log.info("getUserBalances response success={}", response != null && response.isSuccess());
            return response;
        } catch (WebClientResponseException e) {
            log.error("getUserBalances failed status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("getUserBalances error groupName={}", groupName, e);
            return null;
        }
    }

    public Long createExpense(ConversationState state, String userJwt) {
        CreateExpenseRequest request = buildExpenseRequest(state);
        String idempotencyKey = state.getExpenseIdempotencyKey() != null
                ? state.getExpenseIdempotencyKey()
                : "splity-" + state.getUserId() + "-" + state.getCreatedAtEpochMs();
        log.info("createExpense called groupId={} splitType={} totalAmount={} idempotencyKey={} jwtPresent={}",
                state.getGroupId(), state.getSplitType(), state.getTotalAmount(), idempotencyKey, userJwt != null);
        try {
            JsonNode response = webClient.post()
                    .uri("/api/v1/expenses")
                    .header("Authorization", "Bearer " + userJwt)
                    .header("Idempotency-Key", idempotencyKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            log.info("createExpense response: {}", response);
            if (response != null && response.path("success").asBoolean()) {
                JsonNode data = response.path("data");
                if (data.has("expense_id")) return data.get("expense_id").asLong();
            }
            log.warn("Expense creation returned non-success: {}", response);
            return null;
        } catch (WebClientResponseException e) {
            log.error("Expense creation failed status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Expense creation error", e);
            return null;
        }
    }

    private CreateExpenseRequest buildExpenseRequest(ConversationState state) {
        List<CreateExpenseRequest.Participant> participants = new ArrayList<>();

        for (String email : state.getParticipantEmails()) {
            CreateExpenseRequest.Participant.ParticipantBuilder pb = CreateExpenseRequest.Participant.builder()
                    .email(email);

            if ("EXACT".equalsIgnoreCase(state.getSplitType())) {
                pb.exactAmount(state.getExactAmounts().get(email));
            } else if ("PERCENTAGE".equalsIgnoreCase(state.getSplitType())) {
                pb.percentage(state.getPercentages().get(email));
            }
            participants.add(pb.build());
        }

        return CreateExpenseRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .groupId(Long.parseLong(state.getGroupId()))
                .paidByEmail(state.getPaidByEmail())
                .totalAmount(state.getTotalAmount())
                .currency(state.getCurrency())
                .description(state.getDescription())
                .splitType(state.getSplitType())
                .participants(participants)
                .build();
    }
}
