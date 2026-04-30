package com.javaproject.splitewise.service.strategy;

import com.javaproject.splitewise.dto.request.ExpenseParticipantRequest;
import com.javaproject.splitewise.exception.custom.ProcessApiException;
import com.javaproject.splitewise.service.ComputeShareStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public   class PercentageShareStrategy implements ComputeShareStrategy {

    @Override
    public Map<String, String> computeShare(List<ExpenseParticipantRequest> participants, String totalAmount) {
        BigDecimal normalizedTotal = new BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> sharesByUser = new HashMap<>();
        BigDecimal percentageTotal = BigDecimal.ZERO;

        for (ExpenseParticipantRequest participant : participants) {
            BigDecimal percentage = new BigDecimal(participant.getPercentage());
            percentageTotal = percentageTotal.add(percentage);

            BigDecimal amount = normalizedTotal
                    .multiply(percentage)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            sharesByUser.put(participant.getEmail().trim().toLowerCase(Locale.ROOT), amount);
        }

        if (percentageTotal.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("100.00")) != 0) {
            throw new ProcessApiException("INVALID_SPLIT_CONFIGURATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, String> sharesAsString = new HashMap<>();
        sharesByUser.forEach((email, amount) -> sharesAsString.put(email, amount.toPlainString()));
        return sharesAsString;
    }
}
