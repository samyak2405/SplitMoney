package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.dto.request.UserDto;
import com.javaproject.splitewise.dto.response.GetGroupDetailsResponse;
import com.javaproject.splitewise.exception.custom.GroupNotExistsException;
import com.javaproject.splitewise.exception.custom.UserNotInGroupException;
import com.javaproject.splitewise.model.ExpenseGroup;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.ExpenseGroupRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetGroupMembersService {

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public GetGroupDetailsResponse getMembers(Long groupId, UUID userId) {
        log.info("service.group-members.started groupId={} userId={}", groupId, userId);

        ExpenseGroup group = expenseGroupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotExistsException(
                        "Group with id " + groupId + " does not exist", HttpStatus.NOT_FOUND));

        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new UserNotInGroupException(
                        "User is not a member of group " + groupId, HttpStatus.FORBIDDEN));

        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroupId(groupId);
        Map<UUID, User> usersById = userRepository.findAllById(
                groupMembers.stream().map(GroupMember::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        log.info("service.group-members.completed groupId={} memberCount={}", groupId, groupMembers.size());

        return GetGroupDetailsResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .currency(group.getCurrency())
                .memberCount(groupMembers.size())
                .members(groupMembers.stream()
                        .map(m -> {
                            User u = usersById.get(m.getUserId());
                            return UserDto.builder()
                                    .email(u != null ? u.getEmail() : null)
                                    .build();
                        })
                        .toList())
                .build();
    }
}
