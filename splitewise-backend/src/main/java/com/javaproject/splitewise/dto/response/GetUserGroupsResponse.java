package com.javaproject.splitewise.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetUserGroupsResponse {
    private UUID userId;
    private int groupCount;
    private List<UserGroupSummaryResponse> groups;
}
