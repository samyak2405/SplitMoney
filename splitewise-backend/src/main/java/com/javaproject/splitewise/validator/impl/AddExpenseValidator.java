package com.javaproject.splitewise.validator.impl;

import com.javaproject.splitewise.dto.request.AddExpenseRequest;
import com.javaproject.splitewise.dto.request.ApiRequest;
import com.javaproject.splitewise.dto.request.ExpenseParticipantRequest;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.model.ExpenseGroup;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.model.IdempotencyKey;
import com.javaproject.splitewise.model.SplitType;
import com.javaproject.splitewise.model.User;
import com.javaproject.splitewise.repository.ExpenseGroupRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.repository.IdempotencyKeyRepository;
import com.javaproject.splitewise.repository.UserRepository;
import com.javaproject.splitewise.validator.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class AddExpenseValidator implements Validator {
    private static final String ENDPOINT = "/api/v1/expenses";

    private final ExpenseGroupRepository expenseGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserRepository userRepository;

    @Override
    public void validateRequest(ApiRequest request) {
        if (!(request instanceof AddExpenseRequest addExpenseRequest)) {
            throw new ApiValidationException("Invalid request type for add-expense");
        }

        if (!StringUtils.hasText(addExpenseRequest.getIdempotencyKey())) {
            throw new ApiValidationException("IDEMPOTENCY_KEY_REQUIRED", HttpStatus.BAD_REQUEST);
        }
        if (addExpenseRequest.getIdempotencyKey().length() > 128) {
            throw new ApiValidationException("idempotency key must be <= 128 chars");
        }
        if (addExpenseRequest.getGroupId() == null) {
            throw new ApiValidationException("group_id is required");
        }
        if (!StringUtils.hasText(addExpenseRequest.getPaidByEmail())) {
            throw new ApiValidationException("paid_by_email is required");
        }
        if (!addExpenseRequest.getPaidByEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ApiValidationException("paid_by_email must be a valid email");
        }
        if (!StringUtils.hasText(addExpenseRequest.getCurrency()) || !addExpenseRequest.getCurrency().trim().matches("^[A-Za-z]{3}$")) {
            throw new ApiValidationException("currency must be a valid 3-letter ISO code");
        }
        if (!StringUtils.hasText(addExpenseRequest.getDescription())
                || addExpenseRequest.getDescription().trim().length() > 280) {
            throw new ApiValidationException("description is required and must be <= 280 chars");
        }
        if (addExpenseRequest.getParticipants() == null || addExpenseRequest.getParticipants().isEmpty()) {
            throw new ApiValidationException("participants must not be empty");
        }

        String normalizedPayerEmail = addExpenseRequest.getPaidByEmail().trim().toLowerCase(Locale.ROOT);
        User payer = userRepository.findByEmail(normalizedPayerEmail)
                .orElseThrow(() -> new ApiValidationException("USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        Optional<IdempotencyKey> existingIdempotencyKey = idempotencyKeyRepository
                .findByActorUserIdAndEndpointAndIdemKey(
                        payer.getId(),
                        ENDPOINT,
                        addExpenseRequest.getIdempotencyKey().trim()
                );
        if (existingIdempotencyKey.isPresent()) {
            throw new ApiValidationException("IDEMPOTENCY_KEY_REUSED", HttpStatus.CONFLICT);
        }

        ExpenseGroup expenseGroup = expenseGroupRepository.findById(addExpenseRequest.getGroupId())
                .orElseThrow(() -> new ApiValidationException("GROUP_NOT_FOUND", HttpStatus.NOT_FOUND));
        String normalizedCurrency = addExpenseRequest.getCurrency().trim().toUpperCase(Locale.ROOT);
        if (!normalizedCurrency.equalsIgnoreCase(expenseGroup.getCurrency())) {
            throw new ApiValidationException("CURRENCY_MISMATCH", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        groupMemberRepository.findByGroupIdAndUserId(addExpenseRequest.getGroupId(), payer.getId())
                .orElseThrow(() -> new ApiValidationException("USER_NOT_IN_GROUP", HttpStatus.FORBIDDEN));

        SplitType splitType = parseSplitType(addExpenseRequest.getSplitType());
        BigDecimal totalAmount = resolveTotalAmountForValidation(addExpenseRequest, splitType);
        validateParticipants(addExpenseRequest, splitType, totalAmount);
    }

    private SplitType parseSplitType(String splitType) {
        try {
            return SplitType.valueOf(splitType.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiValidationException("split_type must be one of EQUAL | EXACT | PERCENTAGE");
        }
    }

    private void validateParticipants(AddExpenseRequest request, SplitType splitType, BigDecimal totalAmount) {
        Set<String> seenParticipantEmails = new HashSet<>();
        BigDecimal exactTotal = BigDecimal.ZERO;
        BigDecimal percentageTotal = BigDecimal.ZERO;

        for (ExpenseParticipantRequest participant : request.getParticipants()) {
            if (participant == null || !StringUtils.hasText(participant.getEmail())) {
                throw new ApiValidationException("every participant must include email");
            }
            String normalizedEmail = participant.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new ApiValidationException("participant email must be valid");
            }
            if (!seenParticipantEmails.add(normalizedEmail)) {
                throw new ApiValidationException("duplicate participants are not allowed");
            }

            if (splitType == SplitType.EXACT) {
                if (!StringUtils.hasText(participant.getExactAmount())) {
                    throw new ApiValidationException("exact_amount is required for EXACT split", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                BigDecimal exactAmount = parseDecimalValue(participant.getExactAmount(), "exact_amount");
                if (exactAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ApiValidationException("exact_amount must be >= 0", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                exactTotal = exactTotal.add(exactAmount);
            }

            if (splitType == SplitType.PERCENTAGE) {
                if (!StringUtils.hasText(participant.getPercentage())) {
                    throw new ApiValidationException("percentage is required for PERCENTAGE split", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                BigDecimal percentage = parseDecimalValue(participant.getPercentage(), "percentage");
                if (percentage.compareTo(BigDecimal.ZERO) < 0
                        || percentage.compareTo(new BigDecimal("100")) > 0) {
                    throw new ApiValidationException("percentage must be between 0 and 100", HttpStatus.UNPROCESSABLE_ENTITY);
                }
                percentageTotal = percentageTotal.add(percentage);
            }
            //Validate wherever participants belong to same group or not
            Optional<User> user = Optional.of(userRepository.findByEmail(normalizedEmail).orElseThrow(() -> new ApiValidationException(String.format("User with Email %s not Found", participant.getEmail()))));
            Optional<GroupMember> groupMember = groupMemberRepository.findByGroupIdAndUserId(request.getGroupId(), user.get().getId());
            if(groupMember.isEmpty()) {
                throw new ApiValidationException(String.format("User with Email %s does not belong to group %s. Please add the member and try again", participant.getEmail(), request.getGroupId()));
            }
        }

        if (splitType == SplitType.EXACT
                && exactTotal.setScale(2, RoundingMode.HALF_UP).compareTo(totalAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new ApiValidationException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (splitType == SplitType.PERCENTAGE
                && percentageTotal.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("100.00")) != 0) {
            throw new ApiValidationException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private BigDecimal resolveTotalAmountForValidation(AddExpenseRequest request, SplitType splitType) {
        if (splitType == SplitType.EXACT && !StringUtils.hasText(request.getTotalAmount())) {
            return sumExactAmounts(request);
        }

        BigDecimal totalAmount = parseDecimalValue(request.getTotalAmount(), "total_amount");
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiValidationException("total_amount must be greater than 0");
        }
        return totalAmount;
    }

    private BigDecimal sumExactAmounts(AddExpenseRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExpenseParticipantRequest participant : request.getParticipants()) {
            if (!StringUtils.hasText(participant.getExactAmount())) {
                throw new ApiValidationException("exact_amount is required for EXACT split", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            BigDecimal exactAmount = parseDecimalValue(participant.getExactAmount(), "exact_amount");
            total = total.add(exactAmount);
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiValidationException("total_amount must be greater than 0");
        }
        return total;
    }

    private BigDecimal parseDecimalValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ApiValidationException(fieldName + " is required");
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new ApiValidationException(fieldName + " must be a valid decimal number");
        }
    }
}
