package com.javaproject.splitewise.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddExpenseRequest extends ApiRequest {

    @JsonProperty("group_id")
    @NotNull(message = "group_id is required")
    private Long groupId;

    @JsonProperty("paid_by_email")
    @NotBlank(message = "paid_by_email is required")
    private String paidByEmail;

    @JsonProperty("total_amount")
    private String totalAmount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotBlank(message = "description is required")
    @Size(min = 1, max = 280, message = "description must be between 1 and 280 characters")
    private String description;

    @JsonProperty("expense_date")
    private LocalDateTime expenseDate;

    @JsonProperty("split_type")
    @NotBlank(message = "split_type is required")
    private String splitType;

    @Valid
    @NotEmpty(message = "participants must not be empty")
    private List<ExpenseParticipantRequest> participants;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;
}
