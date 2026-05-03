package com.splitmoney.document.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiBody<T> {
    private boolean success;
    private String responseMessage;
    private T data;
}
