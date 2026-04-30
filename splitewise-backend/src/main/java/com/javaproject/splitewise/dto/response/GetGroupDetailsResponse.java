package com.javaproject.splitewise.dto.response;

import com.javaproject.splitewise.dto.request.UserDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetGroupDetailsResponse {
    private Long groupId;
    private String groupName;
    private String groupDescription;
    private String currency;
    private String createdBy;
    private LocalDateTime createdDate;
    private int memberCount;
    private List<UserDto> members;
}
