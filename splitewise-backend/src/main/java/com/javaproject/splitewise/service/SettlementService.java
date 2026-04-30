package com.javaproject.splitewise.service;

import com.javaproject.splitewise.dto.request.SettlementCompleteRequest;
import com.javaproject.splitewise.messaging.NotificationEventType;
import com.javaproject.splitewise.messaging.SplitwiseNotificationEvent;
import com.javaproject.splitewise.model.GroupBalance;
import com.javaproject.splitewise.model.GroupMember;
import com.javaproject.splitewise.repository.GroupBalanceRepository;
import com.javaproject.splitewise.repository.GroupMemberRepository;
import com.javaproject.splitewise.service.impl.NotificationOutboxService;
import com.javaproject.splitewise.service.impl.SplitwiseNotificationEventFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupBalanceRepository groupBalanceRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final SplitwiseNotificationEventFactory notificationEventFactory;

    @Transactional
    public void completeSettlement(SettlementCompleteRequest request) {
        UUID payerUserId = request.payerUserId();
        UUID payeeUserId = request.payeeUserId();
        BigDecimal settledAmount = new BigDecimal(request.amount()).setScale(2, RoundingMode.HALF_UP);

        // Find all groups shared between payer and payee
        Set<Long> payerGroups = groupMemberRepository.findAllByUserId(payerUserId)
                .stream().map(GroupMember::getGroupId).collect(Collectors.toSet());
        Set<Long> payeeGroups = groupMemberRepository.findAllByUserId(payeeUserId)
                .stream().map(GroupMember::getGroupId).collect(Collectors.toSet());
        Set<Long> sharedGroups = payerGroups.stream()
                .filter(payeeGroups::contains)
                .collect(Collectors.toSet());

        if (sharedGroups.isEmpty()) {
            log.warn("settlement.complete no shared groups payerUserId={} payeeUserId={}", payerUserId, payeeUserId);
            publishPaymentReceivedNotification(request);
            return;
        }

        // Apply balance adjustment: payer's balance increases (they paid),
        // payee's balance decreases (they received payment). Distribute across
        // shared groups proportionally to the payer's negative balance in each.
        BigDecimal remaining = settledAmount;
        LocalDateTime now = LocalDateTime.now();

        for (Long groupId : sharedGroups) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            GroupBalance payerBalance = groupBalanceRepository
                    .findByGroupIdAndUserId(groupId, payerUserId).orElse(null);
            GroupBalance payeeBalance = groupBalanceRepository
                    .findByGroupIdAndUserId(groupId, payeeUserId).orElse(null);

            if (payerBalance == null || payeeBalance == null) continue;

            BigDecimal currentPayerNet = new BigDecimal(payerBalance.getNetBalance());
            // Only adjust if payer has a negative balance (owes money) in this group
            if (currentPayerNet.compareTo(BigDecimal.ZERO) >= 0) continue;

            BigDecimal adjustment = remaining.min(currentPayerNet.abs());

            payerBalance.setNetBalance(formatAmount(currentPayerNet.add(adjustment)));
            payerBalance.setUpdatedAt(now);
            groupBalanceRepository.save(payerBalance);

            BigDecimal currentPayeeNet = new BigDecimal(payeeBalance.getNetBalance());
            payeeBalance.setNetBalance(formatAmount(currentPayeeNet.subtract(adjustment)));
            payeeBalance.setUpdatedAt(now);
            groupBalanceRepository.save(payeeBalance);

            remaining = remaining.subtract(adjustment);
            log.info("settlement.balance-adjusted groupId={} payerUserId={} payeeUserId={} adjustment={}",
                    groupId, payerUserId, payeeUserId, adjustment);
        }

        publishPaymentReceivedNotification(request);
        log.info("settlement.complete paymentId={} payerUserId={} payeeUserId={} amount={}",
                request.paymentId(), payerUserId, payeeUserId, settledAmount);
    }

    private void publishPaymentReceivedNotification(SettlementCompleteRequest request) {
        try {
            SplitwiseNotificationEvent event = notificationEventFactory.build(
                    NotificationEventType.PAYMENT_RECEIVED,
                    request.paymentId().toString(),
                    "payment",
                    request.paymentId().toString(),
                    request.payerUserId(),
                    null,
                    request.payeeUserId(),
                    null,
                    Map.of(
                            "amount",   request.amount(),
                            "currency", request.currency(),
                            "paymentId", request.paymentId().toString()
                    )
            );
            notificationOutboxService.enqueue(event, "payment.received");
        } catch (Exception ex) {
            log.error("settlement.notification-failed paymentId={} reason={}", request.paymentId(), ex.getMessage());
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
