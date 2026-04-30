package com.javaproject.splitewise.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetUserGroupsRequest extends ApiRequest {
    private UUID userId;
}
