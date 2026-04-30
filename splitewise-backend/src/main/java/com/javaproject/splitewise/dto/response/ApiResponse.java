package com.javaproject.splitewise.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@Builder
public class ApiResponse<T> {
    private String requestId;
    private boolean success;
    private String responseCode;
    private String responseMessage;
    private OffsetDateTime timestamp;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String requestId, boolean success, String responseCode, String responseMessage,
                       OffsetDateTime timestamp, T data) {
        this.requestId = requestId;
        this.success = success;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.timestamp = timestamp;
        this.data = data;
    }
}
