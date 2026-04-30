package com.javaproject.splitewise.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExpenseParticipantRequest {

    @NotBlank(message = "participant email is required")
    private String email;

    @JsonProperty("exact_amount")
    private String exactAmount;

    private String percentage;
}
