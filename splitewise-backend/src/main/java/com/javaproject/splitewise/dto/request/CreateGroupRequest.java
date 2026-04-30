package com.javaproject.splitewise.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequest extends ApiRequest {

    @NotBlank(message = "name is required")
    @Size(max = 120, message = "name must be at most 120 characters")
    private String name;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
    private String currency;

    @Valid
    private UserDto createdBy;

    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    @Valid
    @NotEmpty(message = "members must not be empty")
    private List<UserDto> members;
}
