package com.javaproject.splitewise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetGroupDetailsRequest extends ApiRequest {

    @NotBlank(message = "group name is required")
    @Size(max = 120, message = "group name must be at most 120 characters")
    private String groupName;

    private UUID userId;
}
