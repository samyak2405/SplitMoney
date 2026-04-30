package com.javaproject.splitewise.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExpenseSettlementRequest extends ApiRequest {

    @JsonProperty("group_id")
    @NotNull(message = "group_id is required")
    private Long groupId;

    @JsonProperty("paid_by_email")
    @NotBlank(message = "paid_by_email is required")
    private String paidByEmail;

    @JsonProperty("paid_to")
    @NotBlank(message = "paid_to is required")
    private String paidTo;

    @JsonProperty("amount")
    @NotBlank(message = "amount is required")
    private String amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency;

    /** CARD or UPI — passed to payment-service. */
    @JsonProperty("payment_method")
    @NotBlank(message = "payment_method is required")
    private String paymentMethod;

    /** Frontend URL Hyperswitch redirects to after checkout. */
    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}
