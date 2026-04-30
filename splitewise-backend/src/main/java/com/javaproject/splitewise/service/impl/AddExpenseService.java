package com.javaproject.splitewise.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaproject.splitewise.dto.request.AddExpenseRequest;
import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.ExpenseParticipantRequest;
import com.javaproject.splitewise.dto.response.AddExpenseResponse;
import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.dto.response.ExpenseSplitResponse;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.exception.custom.ProcessApiException;
import com.javaproject.splitewise.model.*;
import com.javaproject.splitewise.messaging.NotificationEventType;
import com.javaproject.splitewise.repository.ExpenseRepository;
import com.javaproject.splitewise.repository.ExpenseSplitRepository;
import com.javaproject.splitewise.repository.GroupBalanceRepository;
import com.javaproject.splitewise.repository.IdempotencyKeyRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.javaproject.splitewise.service.ComputeShareStrategy;
import com.javaproject.splitewise.service.Processor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class AddExpenseService implements Processor {

    private static final String ENDPOINT = "/api/v1/expenses";

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final GroupBalanceRepository groupBalanceRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final NotificationOutboxService notificationOutboxService;
    private final SplitwiseNotificationEventFactory splitwiseNotificationEventFactory;
    private final ComputeShareFactory computeShareFactory;

    @Override
    @Transactional
    public ApiResponse<?> processApiRequest(ApiRequest apiRequest) {
        AddExpenseRequest request = (AddExpenseRequest) apiRequest;
        log.info("service.add-expense.started requestId={} groupId={} payer={} splitType={} participantCount={}",
                request.getRequestId(),
                request.getGroupId(),
                request.getPaidByEmail(),
                request.getSplitType(),
                request.getParticipants() != null ? request.getParticipants().size() : 0);

        String normalizedPayerEmail = request.getPaidByEmail().trim().toLowerCase(Locale.ROOT);
        User payer = userRepository.findByEmail(normalizedPayerEmail)
                .orElseThrow(() -> new ProcessApiException("PAYER_RESOLUTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR));
        String normalizedIdempotencyKey = request.getIdempotencyKey().trim();
        String requestHash = computeRequestHash(request);
        String normalizedCurrency = request.getCurrency().trim().toUpperCase(Locale.ROOT);

        SplitType splitType = parseSplitType(request.getSplitType());
        List<ExpenseParticipantRequest> normalizedParticipants = normalizeParticipantsForSplit(
                request.getParticipants()
        );

        Set<String> participantEmails = normalizedParticipants.stream()
                .map(participant -> participant.getEmail().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<User> participants = userRepository.findByEmailIn(participantEmails);
        Map<String, User> usersByEmail = participants.stream()
                .collect(Collectors.toMap(
                        user -> user.getEmail().toLowerCase(Locale.ROOT),
                        user -> user
                ));
        if (usersByEmail.size() != participantEmails.size()) {
            throw new ApiValidationException("USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        BigDecimal totalAmount = resolveTotalAmount(request, splitType, normalizedParticipants);
        Map<String, String> computedSharesByEmail = computeShares(normalizedParticipants, totalAmount, splitType);
        Map<UUID, BigDecimal> computedSharesByUserId = new HashMap<>();
        computedSharesByEmail.forEach((email, share) ->
                computedSharesByUserId.put(usersByEmail.get(email).getId(), parseAmount(share)));

        LocalDateTime now = LocalDateTime.now();
        Expense expense = Expense.builder()
                .groupId(request.getGroupId())
                .paidByUserId(payer.getId())
                .totalAmount(formatAmount(totalAmount))
                .currency(normalizedCurrency)
                .description(request.getDescription().trim())
                .splitType(splitType)
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : now)
                .createdByUserId(payer.getId())
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        Map<String, String> percentagesByEmail = extractPercentages(normalizedParticipants);
        List<ExpenseSplit> splitsToSave = new ArrayList<>();
        for (Map.Entry<String, String> entry : computedSharesByEmail.entrySet()) {
            User participantUser = usersByEmail.get(entry.getKey());
            String percentage = splitType == SplitType.PERCENTAGE ? percentagesByEmail.get(entry.getKey()) : null;
            splitsToSave.add(ExpenseSplit.builder()
                    .expenseId(savedExpense.getExpenseId())
                    .userId(participantUser.getId())
                    .shareAmount(formatAmount(parseAmount(entry.getValue())))
                    .sharePercentage(percentage != null ? formatPercentage(parseAmount(percentage)) : null)
                    .build());
        }
        expenseSplitRepository.saveAll(splitsToSave);

        applyBalanceChanges(request.getGroupId(), payer.getId(), totalAmount, computedSharesByUserId, now);

        AddExpenseResponse data = AddExpenseResponse.builder()
                .expenseId(savedExpense.getExpenseId())
                .groupId(savedExpense.getGroupId())
                .paidByEmail(payer.getEmail())
                .totalAmount(savedExpense.getTotalAmount())
                .currency(savedExpense.getCurrency())
                .description(savedExpense.getDescription())
                .splitType(savedExpense.getSplitType().name())
                .splits(computedSharesByEmail.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> ExpenseSplitResponse.builder()
                                .email(entry.getKey())
                                .shareAmount(formatAmount(parseAmount(entry.getValue())))
                                .build())
                        .toList())
                .createdAt(savedExpense.getCreatedAt() != null ? savedExpense.getCreatedAt() : now)
                .build();

        ApiResponse<?> response = ApiResponse.builder()
                .requestId(request.getRequestId())
                .success(true)
                .responseCode(String.valueOf(HttpStatus.CREATED.value()))
                .responseMessage("Expense added successfully")
                .timestamp(OffsetDateTime.now())
                .data(data)
                .build();

        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .actorUserId(payer.getId())
                .endpoint(ENDPOINT)
                .idemKey(normalizedIdempotencyKey)
                .requestHash(requestHash)
                .responseStatus((short) HttpStatus.CREATED.value())
                .responseBody(objectMapper.valueToTree(response))
                .expiresAt(now.plusHours(48))
                .build());

        for (Map.Entry<String, String> entry : computedSharesByEmail.entrySet()) {
            String participantEmail = entry.getKey();
            if (participantEmail.equalsIgnoreCase(payer.getEmail())) {
                continue;
            }
            User participantUser = usersByEmail.get(participantEmail);
            notificationOutboxService.enqueue(
                    splitwiseNotificationEventFactory.build(
                            NotificationEventType.EXPENSE_ADDED_AGAINST_USER,
                            request.getRequestId(),
                            "expense",
                            String.valueOf(savedExpense.getExpenseId()),
                            payer.getId(),
                            payer.getEmail(),
                            participantUser.getId(),
                            participantUser.getEmail(),
                            Map.of(
                                    "groupId", savedExpense.getGroupId(),
                                    "expenseId", savedExpense.getExpenseId(),
                                    "description", savedExpense.getDescription(),
                                    "currency", savedExpense.getCurrency(),
                                    "shareAmount", formatAmount(parseAmount(entry.getValue()))
                            )
                    ),
                    "expense.added.against_user"
            );
        }

        log.info("service.add-expense.completed requestId={} expenseId={} groupId={} splits={}",
                request.getRequestId(),
                savedExpense.getExpenseId(),
                savedExpense.getGroupId(),
                splitsToSave.size());
        return response;
    }

    private SplitType parseSplitType(String splitType) {
        try {
            return SplitType.valueOf(splitType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ProcessApiException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Map<String, String> computeShares(
            List<ExpenseParticipantRequest> participants,
            BigDecimal totalAmount,
            SplitType splitType
    ) {
        List<ExpenseParticipantRequest> orderedParticipants = participants.stream()
                .sorted(Comparator.comparing(participant -> participant.getEmail().trim().toLowerCase(Locale.ROOT)))
                .toList();
        ComputeShareStrategy computeShareStrategy = computeShareFactory.getComputeShareStrategy(splitType);
        return computeShareStrategy.computeShare(orderedParticipants, formatAmount(totalAmount));
    }

    private List<ExpenseParticipantRequest> normalizeParticipantsForSplit(
            List<ExpenseParticipantRequest> participants
    ) {
        Map<String, ExpenseParticipantRequest> uniqueParticipants = new LinkedHashMap<>();
        for (ExpenseParticipantRequest participant : participants) {
            String normalizedEmail = participant.getEmail().trim().toLowerCase(Locale.ROOT);
            uniqueParticipants.putIfAbsent(normalizedEmail, participant);
        }
        return new ArrayList<>(uniqueParticipants.values());
    }

    private Map<String, String> extractPercentages(List<ExpenseParticipantRequest> participants) {
        Map<String, String> percentageByUser = new HashMap<>();
        for (ExpenseParticipantRequest participant : participants) {
            if (participant.getPercentage() != null && !participant.getPercentage().trim().isEmpty()) {
                percentageByUser.put(
                        participant.getEmail().trim().toLowerCase(Locale.ROOT),
                        participant.getPercentage().trim()
                );
            }
        }
        return percentageByUser;
    }

    private void applyBalanceChanges(
            Long groupId,
            UUID payerId,
            BigDecimal totalAmount,
            Map<UUID, BigDecimal> sharesByUser,
            LocalDateTime now
    ) {
        Map<UUID, BigDecimal> deltasByUser = new HashMap<>();
        sharesByUser.forEach((userId, shareAmount) -> deltasByUser.merge(userId, shareAmount.negate(), BigDecimal::add));
        deltasByUser.merge(payerId, totalAmount, BigDecimal::add);

        deltasByUser.forEach((userId, delta) -> {
            GroupBalance groupBalance = groupBalanceRepository.findByGroupIdAndUserId(groupId, userId)
                    .orElse(GroupBalance.builder()
                            .groupId(groupId)
                            .userId(userId)
                            .netBalance("0.00")
                            .updatedAt(now)
                            .build());

            BigDecimal existingBalance = parseAmount(groupBalance.getNetBalance());
            groupBalance.setNetBalance(formatAmount(existingBalance.add(delta)));
            groupBalance.setUpdatedAt(now);
            groupBalanceRepository.save(groupBalance);
        });
    }

    private String computeRequestHash(AddExpenseRequest request) {
        StringBuilder participantText = new StringBuilder();
        request.getParticipants().stream()
                .sorted(Comparator.comparing(participant -> participant.getEmail().trim().toLowerCase(Locale.ROOT)))
                .forEach(participant -> participantText
                        .append(participant.getEmail().trim().toLowerCase(Locale.ROOT)).append("|")
                        .append(participant.getExactAmount() != null ? participant.getExactAmount().trim() : "null")
                        .append("|")
                        .append(participant.getPercentage() != null ? participant.getPercentage().trim() : "null")
                        .append(";"));

        String normalized = request.getGroupId() + "|"
                + request.getPaidByEmail().trim().toLowerCase(Locale.ROOT) + "|"
                + formatAmount(resolveTotalAmount(request, parseSplitType(request.getSplitType()), request.getParticipants())) + "|"
                + request.getCurrency().trim().toUpperCase(Locale.ROOT) + "|"
                + request.getDescription().trim() + "|"
                + (request.getExpenseDate() != null ? request.getExpenseDate() : "null") + "|"
                + request.getSplitType().trim().toUpperCase(Locale.ROOT) + "|"
                + participantText;
        return sha256(normalized);
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ProcessApiException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new ProcessApiException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal resolveTotalAmount(
            AddExpenseRequest request,
            SplitType splitType,
            List<ExpenseParticipantRequest> participants
    ) {
        if (splitType == SplitType.EXACT && !StringUtils.hasText(request.getTotalAmount())) {
            BigDecimal total = BigDecimal.ZERO;
            for (ExpenseParticipantRequest participant : participants) {
                total = total.add(parseAmount(participant.getExactAmount()));
            }
            return total;
        }
        return parseAmount(request.getTotalAmount());
    }

    private String formatPercentage(BigDecimal percentage) {
        return percentage.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new ProcessApiException("Unable to compute request hash", HttpStatus.INTERNAL_SERVER_ERROR, ex);
        }
    }
}