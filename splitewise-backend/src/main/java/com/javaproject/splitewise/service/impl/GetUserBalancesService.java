package com.javaproject.splitewise.service.impl;

import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.GetUserBalancesRequest;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.GetUserBalancesResponse;
import com.javaproject.splitewise.dto.response.UserBalanceDetailResponse;
import com.javaproject.splitewise.exception.custom.GroupNotExistsException;
import com.javaproject.splitewise.exception.custom.UserNotFoundException;
import com.javaproject.splitewise.exception.custom.UserNotInGroupException;
import com.javaproject.splitewise.model.ExpenseGroup;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.ExpenseGroupRepository;
import com.javaproject.splitewise.repository.GroupBalanceRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.javaproject.splitewise.service.Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserBalancesService implements Processor {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final UserRepository userRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupBalanceRepository groupBalanceRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        GetUserBalancesRequest request = (GetUserBalancesRequest) apiRequest;
        String groupName = request.getGroupName().trim();
        String userEmail = request.getUserEmail().trim().toLowerCase(Locale.ROOT);
        log.info("service.user-balances.started requestId={} groupName={} userEmail={}",
                request.getRequestId(), groupName, userEmail);

        ExpenseGroup group = expenseGroupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new GroupNotExistsException(
                        "Group with name " + groupName + " does not exist",
                        HttpStatus.NOT_FOUND
                ));
        Long groupId = group.getGroupId();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "User with email " + userEmail + " not found",
                        HttpStatus.NOT_FOUND
                ));
        UUID userId = user.getId();

        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new UserNotInGroupException(
                        "User with email " + userEmail + " is not a member of group " + groupName,
                        HttpStatus.FORBIDDEN
                ));

        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);
        Map<UUID, User> usersById = userRepository.findAllById(
                        members.stream().map(GroupMember::getUserId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(User::getId, memberUser -> memberUser));

        Map<UUID, BigDecimal> balancesByUser = new HashMap<>();
        members.stream()
                .map(GroupMember::getUserId)
                .distinct()
                .forEach(memberUserId -> balancesByUser.put(memberUserId, ZERO));

        groupBalanceRepository.findAllByGroupId(groupId)
                .forEach(groupBalance -> balancesByUser.put(
                        groupBalance.getUserId(),
                        scale(parseAmount(groupBalance.getNetBalance()))
                ));

        BigDecimal userNetBalance = balancesByUser.getOrDefault(userId, ZERO);
        BigDecimal userReceivable = userNetBalance.max(ZERO);
        BigDecimal userPayable = userNetBalance.min(ZERO).abs();

        List<UserBalanceDetailResponse> membersWhoNeedToPayUser = new ArrayList<>();
        List<UserBalanceDetailResponse> membersUserNeedsToPay = new ArrayList<>();

        if (userReceivable.compareTo(ZERO) > 0) {
            BigDecimal remainingReceivable = userReceivable;
            List<Map.Entry<UUID, BigDecimal>> debtors = balancesByUser.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(userId))
                    .filter(entry -> entry.getValue().compareTo(ZERO) < 0)
                    .sorted(Comparator.comparing(entry -> {
                        User debtor = usersById.get(entry.getKey());
                        return debtor != null ? debtor.getEmail() : entry.getKey().toString();
                    }))
                    .toList();

            for (Map.Entry<UUID, BigDecimal> debtorEntry : debtors) {
                if (remainingReceivable.compareTo(ZERO) <= 0) {
                    break;
                }
                BigDecimal debtorCanPay = debtorEntry.getValue().abs();
                BigDecimal settledAmount = remainingReceivable.min(debtorCanPay);
                if (settledAmount.compareTo(ZERO) > 0) {
                    User debtor = usersById.get(debtorEntry.getKey());
                    membersWhoNeedToPayUser.add(UserBalanceDetailResponse.builder()
                            .email(debtor != null ? debtor.getEmail() : null)
                            .amount(toAmountString(scale(settledAmount)))
                            .build());
                    remainingReceivable = remainingReceivable.subtract(settledAmount);
                }
            }
        } else if (userPayable.compareTo(ZERO) > 0) {
            BigDecimal remainingPayable = userPayable;
            List<Map.Entry<UUID, BigDecimal>> creditors = balancesByUser.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(userId))
                    .filter(entry -> entry.getValue().compareTo(ZERO) > 0)
                    .sorted(Comparator.comparing(entry -> {
                        User creditor = usersById.get(entry.getKey());
                        return creditor != null ? creditor.getEmail() : entry.getKey().toString();
                    }))
                    .toList();

            for (Map.Entry<UUID, BigDecimal> creditorEntry : creditors) {
                if (remainingPayable.compareTo(ZERO) <= 0) {
                    break;
                }
                BigDecimal creditorCanReceive = creditorEntry.getValue();
                BigDecimal settledAmount = remainingPayable.min(creditorCanReceive);
                if (settledAmount.compareTo(ZERO) > 0) {
                    User creditor = usersById.get(creditorEntry.getKey());
                    membersUserNeedsToPay.add(UserBalanceDetailResponse.builder()
                            .email(creditor != null ? creditor.getEmail() : null)
                            .amount(toAmountString(scale(settledAmount)))
                            .build());
                    remainingPayable = remainingPayable.subtract(settledAmount);
                }
            }
        }

        log.info("service.user-balances.completed requestId={} groupName={} userEmail={} receivableCount={} payableCount={}",
                request.getRequestId(),
                groupName,
                userEmail,
                membersWhoNeedToPayUser.size(),
                membersUserNeedsToPay.size());

        return ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.OK.value()))
                .responseMessage("User balances fetched successfully")
                .timestamp(OffsetDateTime.now())
                .data(GetUserBalancesResponse.builder()
                        .groupName(group.getGroupName())
                        .userEmail(user.getEmail())
                        .currency(group.getCurrency())
                        .totalToReceive(toAmountString(scale(userReceivable)))
                        .totalToPay(toAmountString(scale(userPayable)))
                        .membersWhoNeedToPayUserCount(membersWhoNeedToPayUser.size())
                        .membersUserNeedsToPayCount(membersUserNeedsToPay.size())
                        .membersWhoNeedToPayUser(membersWhoNeedToPayUser)
                        .membersUserNeedsToPay(membersUserNeedsToPay)
                        .build())
                .build();
    }

    private BigDecimal scale(BigDecimal amount) {
        return (amount == null ? ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            return ZERO;
        }
        return new BigDecimal(amount.trim());
    }

    private String toAmountString(BigDecimal amount) {
        return scale(amount).toPlainString();
    }
}
