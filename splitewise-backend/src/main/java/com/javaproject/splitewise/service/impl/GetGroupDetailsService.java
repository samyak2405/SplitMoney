package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetGroupDetailsRequest;
import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.GetGroupDetailsResponse;
import com.javaproject.splitewise.exception.custom.GroupNotExistsException;
import com.javaproject.splitewise.exception.custom.UserNotFoundException;
import com.javaproject.splitewise.exception.custom.UserNotInGroupException;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetGroupDetailsService implements Processor {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        GetGroupDetailsRequest request = (GetGroupDetailsRequest) apiRequest;
        log.info("service.group-details.started requestId={} groupName={} userId={}",
                request.getRequestId(), request.getGroupName(), request.getUserId());

        ExpenseGroup expenseGroup = expenseGroupRepository.findByGroupName(request.getGroupName().trim())
                .orElseThrow(() -> new GroupNotExistsException(
                        "Group with name " + request.getGroupName() + " does not exist",
                        HttpStatus.NOT_FOUND
                ));

        UUID userId = request.getUserId();
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + userId + " not found",
                        HttpStatus.NOT_FOUND
                ));

        groupMemberRepository.findByGroupIdAndUserId(expenseGroup.getGroupId(), userId)
                .orElseThrow(() -> new UserNotInGroupException(
                        "User with id " + userId + " is not a member of the group " + request.getGroupName(),
                        HttpStatus.FORBIDDEN
                ));

        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroupId(expenseGroup.getGroupId());
        Map<UUID, User> usersById = userRepository.findAllById(
                groupMembers.stream().map(GroupMember::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        log.info("service.group-details.completed requestId={} groupId={} memberCount={}",
                request.getRequestId(),
                expenseGroup.getGroupId(),
                groupMembers.size());

        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("Group details fetched successfully")
                .timestamp(OffsetDateTime.now())
                .data(GetGroupDetailsResponse.builder()
                        .groupId(expenseGroup.getGroupId())
                        .groupName(expenseGroup.getGroupName())
                        .groupDescription(expenseGroup.getDescription())
                        .currency(expenseGroup.getCurrency())
                        .createdBy(expenseGroup.getCreatedBy().getEmail())
                        .createdDate(expenseGroup.getCreatedAt())
                        .memberCount(groupMembers.size())
                        .members(groupMembers.stream()
                                .map(member -> {
                                    User user = usersById.get(member.getUserId());
                                    return UserDto.builder()
                                            .email(user != null ? user.getEmail() : null)
                                            .build();
                                })
                                .toList())
                        .build())
                .build();
    }
}
