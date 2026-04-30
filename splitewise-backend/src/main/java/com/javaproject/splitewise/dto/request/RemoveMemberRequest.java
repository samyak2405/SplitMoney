package com.javaproject.splitewise.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RemoveMemberRequest extends ApiRequest{

    @Valid
    private UserDto memberToRemove;
    @NotBlank(message = "group name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    private String groupName;
    @Valid
    private UserDto removedBy;
}
