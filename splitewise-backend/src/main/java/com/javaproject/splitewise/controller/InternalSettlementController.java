package com.javaproject.splitewise.controller;

import com.javaproject.splitewise.dto.request.SettlementCompleteRequest;
import com.javaproject.splitewise.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/settlements")
@RequiredArgsConstructor
public class InternalSettlementController {

    private final SettlementService settlementService;

    /**
     * Called by payment-service when a payment reaches COMPLETED status.
     * Updates group balances and publishes a PAYMENT_RECEIVED notification.
     */
    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.OK)
    public void complete(@Valid @RequestBody SettlementCompleteRequest request) {
        log.info("settlement.complete.received paymentId={} payerUserId={} payeeUserId={} amount={}",
                request.paymentId(), request.payerUserId(), request.payeeUserId(), request.amount());
        settlementService.completeSettlement(request);
    }
}
