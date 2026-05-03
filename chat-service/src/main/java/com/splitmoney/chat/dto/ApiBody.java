package com.splitmoney.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiBody<T> {
    private boolean success;
    private String responseCode;
    private String responseMessage;
    private T data;
}
