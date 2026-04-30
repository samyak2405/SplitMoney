package com.splitmoney.application.api.dto;

import java.util.List;

public record PaymentListResponse(
        List<PaymentResponse> payments,
        PageInfo pageInfo
) {
    public record PageInfo(boolean hasNextPage, String endCursor) {}
}
