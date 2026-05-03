package com.splitmoney.ai.splitwise.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserBalancesResponse {

    private boolean success;
    private Data data;

    @lombok.Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String groupName;
        private String userEmail;
        private String currency;
        private String totalToReceive;
        private String totalToPay;
        private List<BalanceItem> membersWhoNeedToPayUser;
        private List<BalanceItem> membersUserNeedsToPay;
    }

    @lombok.Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BalanceItem {
        private String email;
        private String amount;
    }
}
