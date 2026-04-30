package com.javaproject.splitewise.dto.response;

import com.javaproject.splitewise.dto.request.UserDto;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RemoveMemberFromGroupResponse {

    private Long groupId;
    private UserDto memberId;
    private LocalDateTime removedDate;
}
