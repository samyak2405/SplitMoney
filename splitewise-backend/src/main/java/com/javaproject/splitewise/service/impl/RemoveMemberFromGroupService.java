package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.RemoveMemberRequest;
import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.RemoveMemberFromGroupResponse;
import com.javaproject.splitewise.exception.custom.GroupNotExistsException;
import com.javaproject.splitewise.exception.custom.UserNotFoundException;
import com.javaproject.splitewise.exception.custom.UserNotInGroupException;
import com.javaproject.splitewise.model.ExpenseGroup;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.messaging.NotificationEventType;
import com.javaproject.splitewise.repository.ExpenseGroupRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.javaproject.splitewise.service.Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RemoveMemberFromGroupService implements Processor {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final SplitwiseNotificationEventFactory splitwiseNotificationEventFactory;
    @Override
    public ApiResponse<RemoveMemberFromGroupResponse> processApiRequest(ApiRequest apiRequest) {
        RemoveMemberRequest request = (RemoveMemberRequest) apiRequest;
        log.info("service.remove-member.started requestId={} groupName={} removedBy={} memberToRemove={}",
                request.getRequestId(),
                request.getGroupName(),
                request.getRemovedBy() != null ? request.getRemovedBy().getEmail() : null,
                request.getMemberToRemove() != null ? request.getMemberToRemove().getEmail() : null);

        Optional<ExpenseGroup> expenseGroup = Optional.of(expenseGroupRepository.findByGroupName(request.getGroupName()).orElseThrow(() -> new GroupNotExistsException(
                "Group with name " + request.getGroupName() + " does not exist",
                HttpStatus.NOT_FOUND
        )));

        Optional<User> removedBy = Optional.of(userRepository.findByEmail(request.getRemovedBy().getEmail()).orElseThrow(() -> new UserNotFoundException(
                "User with email " + request.getRemovedBy().getEmail() + " not found",
                HttpStatus.NOT_FOUND
        )));

        Optional<User> memberToRemove = Optional.of(userRepository.findByEmail(request.getMemberToRemove().getEmail()).orElseThrow(() -> new UserNotFoundException(
                "User with email " + request.getMemberToRemove().getEmail() + " not found",
                HttpStatus.NOT_FOUND
        )));

        groupMemberRepository.findByGroupIdAndUserId(expenseGroup.get().getGroupId(), removedBy.get().getId()).orElseThrow(() -> new UserNotInGroupException(
                "User with email " + request.getRemovedBy().getEmail() + " is not a member of the group " + request.getGroupName(),
                HttpStatus.FORBIDDEN
        ));

        GroupMember memberToRemoveGroupMembership = groupMemberRepository
                .findByGroupIdAndUserId(expenseGroup.get().getGroupId(), memberToRemove.get().getId())
                .orElseThrow(() -> new UserNotInGroupException(
                        "User with email " + request.getMemberToRemove().getEmail() + " is not a member of the group " + request.getGroupName(),
                        HttpStatus.NOT_FOUND
                ));

        groupMemberRepository.delete(memberToRemoveGroupMembership);
        notificationOutboxService.enqueue(
                splitwiseNotificationEventFactory.build(
                        NotificationEventType.GROUP_MEMBER_REMOVED,
                        request.getRequestId(),
                        "group",
                        String.valueOf(expenseGroup.get().getGroupId()),
                        removedBy.get().getId(),
                        removedBy.get().getEmail(),
                        memberToRemove.get().getId(),
                        memberToRemove.get().getEmail(),
                        Map.of(
                                "groupId", expenseGroup.get().getGroupId(),
                                "groupName", expenseGroup.get().getGroupName(),
                                "removedBy", removedBy.get().getEmail()
                        )
                ),
                "group.member.removed"
        );

        log.info("service.remove-member.completed requestId={} groupId={} removedMember={}",
                request.getRequestId(),
                expenseGroup.get().getGroupId(),
                memberToRemove.get().getEmail());

        return ApiResponse.<RemoveMemberFromGroupResponse>builder()
                .requestId(request.getRequestId())
                .data(RemoveMemberFromGroupResponse.builder()
                        .groupId(expenseGroup.get().getGroupId())
                        .memberId(UserDto.builder()
                                .email(memberToRemove.get().getEmail())
                                .build())
                        .removedDate(LocalDateTime.now())
                        .build())
                .build();
    }
}
