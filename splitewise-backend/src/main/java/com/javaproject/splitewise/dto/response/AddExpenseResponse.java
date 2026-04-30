package com.javaproject.splitewise.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
@ToString
public class AddExpenseResponse {

    @JsonProperty("expense_id")
    private Long expenseId;

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

    private List<ExpenseSplitResponse> splits;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
