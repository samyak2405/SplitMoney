package com.javaproject.splitewise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ApiRequest {
    @NotBlank(message = "requestId is required")
    @Size(max = 64, message = "requestId must not exceed 64 characters")
    private String requestId;

    @Size(max = 64)
    private String ipAddress;

    @Size(max = 255)
    private String userAgent;
}
