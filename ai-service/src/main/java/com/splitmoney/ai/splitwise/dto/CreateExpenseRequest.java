package com.splitmoney.ai.splitwise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateExpenseRequest {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("paid_by_email")
    private String paidByEmail;

    @JsonProperty("total_amount")
    private String totalAmount;

    private String currency;

    private String description;

    @JsonProperty("split_type")
    private String splitType;

    private List<Participant> participants;

    @Data
    @Builder
    public static class Participant {
        private String email;

        @JsonProperty("exact_amount")
        private String exactAmount;

        private String percentage;
    }
}
