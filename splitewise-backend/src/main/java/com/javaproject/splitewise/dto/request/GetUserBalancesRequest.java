package com.javaproject.splitewise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUserBalancesRequest extends ApiRequest {

    @NotBlank(message = "groupName is required")
    @Size(max = 120, message = "groupName must be at most 120 characters")
    private String groupName;

    private String userEmail;
}
