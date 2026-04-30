package com.javaproject.splitewise.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupSummaryResponse {
    private Long groupId;
    private String groupName;
    private String groupDescription;
    private String currency;
    private String createdBy;
    private LocalDateTime createdDate;
}
