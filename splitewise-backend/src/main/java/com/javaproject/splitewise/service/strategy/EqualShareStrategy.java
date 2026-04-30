package com.javaproject.splitewise.service.strategy;

import com.javaproject.splitewise.dto.request.ExpenseParticipantRequest;
import com.javaproject.splitewise.service.ComputeShareStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EqualShareStrategy implements ComputeShareStrategy {
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Override
    public Map<String, String> computeShare(List<ExpenseParticipantRequest> participants, String totalAmount) {
        if (participants == null || participants.isEmpty()) {
            return Map.of();
        }

        BigDecimal normalizedTotal = new BigDecimal(totalAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal participantCount = BigDecimal.valueOf(participants.size());
        BigDecimal baseShare = normalizedTotal.divide(participantCount, 2, RoundingMode.DOWN);

        Map<String, BigDecimal> sharesByUser = new HashMap<>();
        for (ExpenseParticipantRequest participant : participants) {
            sharesByUser.put(participant.getEmail().trim().toLowerCase(Locale.ROOT), baseShare);
        }

        BigDecimal allocated = sharesByUser.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        int residualCents = normalizedTotal.subtract(allocated).movePointRight(2).intValueExact();
        for (int i = 0; i < residualCents; i++) {
            String email = participants.get(i).getEmail().trim().toLowerCase(Locale.ROOT);
            sharesByUser.put(email, sharesByUser.get(email).add(ONE_CENT));
        }

        Map<String, String> sharesAsString = new HashMap<>();
        sharesByUser.forEach((email, amount) -> sharesAsString.put(email, amount.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        return sharesAsString;
    }
}
