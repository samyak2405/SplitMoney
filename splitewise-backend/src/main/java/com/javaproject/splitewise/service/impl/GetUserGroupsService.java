package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetUserGroupsRequest;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.GetUserGroupsResponse;
import com.javaproject.splitewise.dto.response.UserGroupSummaryResponse;
import com.javaproject.splitewise.exception.custom.UserNotFoundException;
import com.javaproject.splitewise.model.ExpenseGroup;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.ExpenseGroupRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.javaproject.splitewise.service.Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserGroupsService implements Processor {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseGroupRepository expenseGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        GetUserGroupsRequest request = (GetUserGroupsRequest) apiRequest;
        UUID userId = request.getUserId();
        log.info("service.user-groups.started requestId={} userId={}", request.getRequestId(), userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + userId + " not found",
                        HttpStatus.NOT_FOUND
                ));

        List<GroupMember> memberships = groupMemberRepository.findAllByUserId(userId);
        List<Long> groupIds = memberships.stream()
                .map(GroupMember::getGroupId)
                .distinct()
                .toList();

        List<ExpenseGroup> groups = expenseGroupRepository.findAllById(groupIds);
        Map<Long, ExpenseGroup> groupsById = groups.stream()
                .collect(Collectors.toMap(ExpenseGroup::getGroupId, Function.identity()));

        List<UserGroupSummaryResponse> responseGroups = groupIds.stream()
                .map(groupsById::get)
                .filter(Objects::nonNull)
                .map(group -> UserGroupSummaryResponse.builder()
                        .groupId(group.getGroupId())
                        .groupName(group.getGroupName())
                        .groupDescription(group.getDescription())
                        .currency(group.getCurrency())
                        .createdBy(group.getCreatedBy().getEmail())
                        .createdDate(group.getCreatedAt())
                        .build())
                .toList();

        log.info("service.user-groups.completed requestId={} userId={} groupCount={}",
                request.getRequestId(),
                userId,
                responseGroups.size());

        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("User groups fetched successfully")
                .timestamp(OffsetDateTime.now())
                .data(GetUserGroupsResponse.builder()
                        .userId(userId)
                        .groupCount(responseGroups.size())
                        .groups(responseGroups)
                        .build())
                .build();
    }
}
