package com.javaproject.splitewise.dto.response;


import com.javaproject.splitewise.dto.request.UserDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CreateGroupResponse {

    private Long groupId;
    private String groupName;
    private String groupDescription;
    private String currency;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private String updatedDate;
    private int memberCount;
    private List<UserDto> members;
}
