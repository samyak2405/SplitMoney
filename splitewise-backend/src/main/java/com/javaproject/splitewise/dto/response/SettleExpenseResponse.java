package com.javaproject.splitewise.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleExpenseResponse {

    @JsonProperty("saga_id")
    private UUID sagaId;

    @JsonProperty("payment_id")
    private UUID paymentId;

    @JsonProperty("checkout_url")
    private String checkoutUrl;

    @JsonProperty("status")
    private String status;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("paid_by_email")
    private String paidByEmail;

    @JsonProperty("paid_to_email")
    private String paidToEmail;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("payment_method")
    private String paymentMethod;
}
