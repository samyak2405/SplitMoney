package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.CreateGroupRequest;
import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class CreateGroupValidator implements Validator {
    @Override
    public void validateRequest(ApiRequest request) {
        if (!(request instanceof CreateGroupRequest createGroupRequest)) {
            throw new ApiValidationException("Invalid request type for create-group");
        }

        if (createGroupRequest.getCreatedBy() == null
                || !StringUtils.hasText(createGroupRequest.getCreatedBy().getEmail())) {
            throw new ApiValidationException("createdBy.email is required");
        }

        List<UserDto> members = createGroupRequest.getMembers();
        if (members == null || members.isEmpty()) {
            throw new ApiValidationException("members must contain at least one user");
        }

        long invalidMemberEntries = members.stream()
                .filter(member -> member == null || !StringUtils.hasText(member.getEmail()))
                .count();
        if (invalidMemberEntries > 0) {
            throw new ApiValidationException("all members must include a valid email");
        }

        Set<String> normalizedEmails = members.stream()
                .map(member -> member.getEmail().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (normalizedEmails.size() != members.size()) {
            throw new ApiValidationException("duplicate member emails are not allowed");
        }

        String currency = createGroupRequest.getCurrency();
        if (StringUtils.hasText(currency) && !currency.trim().matches("^[A-Za-z]{3}$")) {
            throw new ApiValidationException("currency must be a valid 3-letter ISO code");
        }
    }
}
