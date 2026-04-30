package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.constants.SplitwiseAppConstants;
import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.CreateGroupRequest;
import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.CreateGroupResponse;
import com.javaproject.splitewise.exception.custom.GroupAdminNotFoundException;
import com.javaproject.splitewise.exception.custom.GroupAlreadyExistsException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateGroupService implements Processor {
    private final UserRepository userRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final SplitwiseNotificationEventFactory splitwiseNotificationEventFactory;

    @Override
    @Transactional
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        CreateGroupRequest request = (CreateGroupRequest) apiRequest;
        String normalizedGroupName = request.getName().trim();
        log.info("service.create-group.started requestId={} groupName={} createdBy={}",
                request.getRequestId(),
                normalizedGroupName,
                request.getCreatedBy() != null ? request.getCreatedBy().getEmail() : null);
        if (expenseGroupRepository.existsByGroupName(normalizedGroupName)) {
            throw new GroupAlreadyExistsException(
                    "Group with name " + normalizedGroupName + " already exists",
                    HttpStatus.CONFLICT
            );
        }

        UserDto createdByDto = request.getCreatedBy();
        String creatorEmail = createdByDto.getEmail().trim();
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new GroupAdminNotFoundException(
                        "Group admin with email " + creatorEmail + " not found",
                        HttpStatus.NOT_FOUND
                ));

        Set<String> uniqueMemberEmails = new LinkedHashSet<>();
        for (UserDto member : request.getMembers()) {
            if (member != null && StringUtils.hasText(member.getEmail())) {
                uniqueMemberEmails.add(member.getEmail().trim());
            }
        }
        uniqueMemberEmails.add(creatorEmail);

        List<User> resolvedUsers = userRepository.findByEmailIn(uniqueMemberEmails);
        Map<String, User> usersByEmail = resolvedUsers.stream()
                .collect(Collectors.toMap(
                        user -> user.getEmail().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));

        List<String> missingEmails = uniqueMemberEmails.stream()
                .filter(email -> !usersByEmail.containsKey(email.toLowerCase(Locale.ROOT)))
                .toList();
        if (!missingEmails.isEmpty()) {
            throw new GroupAdminNotFoundException(
                    "Users not found for emails: " + String.join(", ", missingEmails),
                    HttpStatus.NOT_FOUND
            );
        }

        String normalizedCurrency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim().toUpperCase(Locale.ROOT)
                : "INR";

        ExpenseGroup expenseGroup = ExpenseGroup.builder()
                .createdBy(creator)
                .groupName(normalizedGroupName)
                .description(request.getDescription())
                .currency(normalizedCurrency)
                .build();

        ExpenseGroup savedExpenseGroup = expenseGroupRepository.save(expenseGroup);

        List<GroupMember> groupMembers = new ArrayList<>();
        uniqueMemberEmails.forEach(email -> {
            User memberUser = usersByEmail.get(email.toLowerCase(Locale.ROOT));
            groupMembers.add(GroupMember.builder()
                    .group(savedExpenseGroup)
                    .groupId(savedExpenseGroup.getGroupId())
                    .addedBy(creator)
                    .role(SplitwiseAppConstants.MEMBER)
                    .addedByUserId(creator.getId())
                    .userId(memberUser.getId())
                    .joinedAt(LocalDateTime.now())
                    .user(memberUser)
                    .build());
        });
        groupMemberRepository.saveAll(groupMembers);

        for (GroupMember member : groupMembers) {
            if (member.getUserId().equals(creator.getId())) continue;
            notificationOutboxService.enqueue(
                    splitwiseNotificationEventFactory.build(
                            NotificationEventType.GROUP_MEMBER_ADDED,
                            request.getRequestId(),
                            "group",
                            String.valueOf(savedExpenseGroup.getGroupId()),
                            creator.getId(),
                            creator.getEmail(),
                            member.getUserId(),
                            member.getUser().getEmail(),
                            Map.of(
                                    "groupId", savedExpenseGroup.getGroupId(),
                                    "groupName", savedExpenseGroup.getGroupName(),
                                    "addedBy", creator.getEmail()
                            )
                    ),
                    "group.member.added"
            );
        }

        log.info("service.create-group.completed requestId={} groupId={} memberCount={}",
                request.getRequestId(),
                savedExpenseGroup.getGroupId(),
                groupMembers.size());

        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.CREATED.value()))
                .responseMessage("Group created successfully")
                .timestamp(OffsetDateTime.now())
                .data(CreateGroupResponse.builder()
                        .createdBy(creator.getEmail())
                        .createdDate(savedExpenseGroup.getCreatedAt())
                        .groupDescription(savedExpenseGroup.getDescription())
                        .groupName(savedExpenseGroup.getGroupName())
                        .currency(expenseGroup.getCurrency())
                        .members(groupMembers.stream()
                                .map(member -> UserDto.builder().email(member.getUser().getEmail()).build())
                                .toList())
                        .memberCount(groupMembers.size())
                        .groupId(expenseGroup.getGroupId())
                        .build())
                .build();
    }
}
