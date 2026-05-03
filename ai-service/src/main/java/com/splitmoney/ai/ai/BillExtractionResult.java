package com.splitmoney.ai.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillExtractionResult {

    private String action;      // ASK | READY | CLARIFY | CANCEL | QUERY | TEXT
    private String reply;       // human-readable text to send in chat
    private String queryType;   // BALANCES | GENERAL (used when action=QUERY)
    private String querySubject; // optional — specific member email, etc.

    private Extracted extracted;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extracted {
        private String description;
        private String totalAmount;
        private String currency;
        private String paidByEmail;
        private String splitType;
        private List<Participant> participants;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Participant {
        private String email;
        private String amount;
        private String percentage;
    }
}
