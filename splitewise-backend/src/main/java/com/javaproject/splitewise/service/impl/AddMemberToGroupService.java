package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.constants.SplitwiseAppConstants;
import com.javaproject.splitewise.dto.request.AddMemberToGroupRequest;
import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.dto.response.AddMemberToGroupResponse;
import com.javaproject.splitewise.dto.response.ApiResponse;
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
@Service
@RequiredArgsConstructor
public class AddMemberToGroupService implements Processor {
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final SplitwiseNotificationEventFactory splitwiseNotificationEventFactory;
    @Override
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        AddMemberToGroupRequest request = (AddMemberToGroupRequest) apiRequest;
        log.info("service.add-member.started requestId={} groupName={} addedBy={} memberToAdd={}",
                request.getRequestId(),
                request.getGroupName(),
                request.getAddedBy() != null ? request.getAddedBy().getEmail() : null,
                request.getMemberToAdd() != null ? request.getMemberToAdd().getEmail() : null);
        Optional<ExpenseGroup> expenseGroup = Optional.of(expenseGroupRepository.findByGroupName(request.getGroupName()).orElseThrow(() -> new GroupNotExistsException(
                "Group with name " + request.getGroupName() + " does not exist",
                HttpStatus.NOT_FOUND
        )));

        Optional<User> addedBy = Optional.of(userRepository.findByEmail(request.getAddedBy().getEmail()).orElseThrow(() -> new UserNotFoundException(
                "User with email " + request.getAddedBy().getEmail() + " not found",
                HttpStatus.NOT_FOUND
        )));

        Optional<User> memberToAdd = Optional.of(userRepository.findByEmail(request.getMemberToAdd().getEmail()).orElseThrow(() -> new UserNotFoundException(
                "User with email " + request.getMemberToAdd().getEmail() + " not found",
                HttpStatus.NOT_FOUND
        )));

        Optional<GroupMember> groupMember  = Optional.of(groupMemberRepository.findByGroupIdAndUserId(expenseGroup.get().getGroupId(), addedBy.get().getId()).orElseThrow(() -> new UserNotInGroupException(
                "User with email " + request.getAddedBy().getEmail() + " is not a member of the group " + request.getGroupName(),
                HttpStatus.FORBIDDEN
        )));
        GroupMember saveNewMember = GroupMember.builder()
                .group(expenseGroup.get())
                .groupId(expenseGroup.get().getGroupId())
                .addedBy(addedBy.get())
                .role(SplitwiseAppConstants.MEMBER)
                .addedByUserId(addedBy.get().getId())
                .userId(memberToAdd.get().getId())
                .joinedAt(LocalDateTime.now())
                .user(memberToAdd.get())
                .build();
        groupMemberRepository.save(saveNewMember);
        notificationOutboxService.enqueue(
                splitwiseNotificationEventFactory.build(
                        NotificationEventType.GROUP_MEMBER_ADDED,
                        request.getRequestId(),
                        "group",
                        String.valueOf(expenseGroup.get().getGroupId()),
                        addedBy.get().getId(),
                        addedBy.get().getEmail(),
                        memberToAdd.get().getId(),
                        memberToAdd.get().getEmail(),
                        Map.of(
                                "groupId", expenseGroup.get().getGroupId(),
                                "groupName", expenseGroup.get().getGroupName(),
                                "addedBy", addedBy.get().getEmail()
                        )
                ),
                "group.member.added"
        );
        log.info("service.add-member.completed requestId={} groupId={} addedMember={}",
                request.getRequestId(),
                expenseGroup.get().getGroupId(),
                memberToAdd.get().getEmail());
        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .data(AddMemberToGroupResponse.builder()
                        .memberId(UserDto.builder()
                                .email(memberToAdd.get().getEmail())
                                .build())
                        .addedDate(LocalDateTime.now())
                        .groupId(expenseGroup.get().getGroupId())
                        .build())
                .build();
    }
}
