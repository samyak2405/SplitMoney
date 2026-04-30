package com.splitmoney.application.infra.settlement;

import com.splitmoney.application.domain.payment.Payment;
import com.splitmoney.application.grpc.SettlementCallbackGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delegates settlement completion / failure notifications to the
 * splitwise-backend via gRPC (replaced the previous HTTP RestClient call).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementNotifier {

    private final SettlementCallbackGrpcClient grpcClient;

    public void notifySettlementComplete(Payment payment) {
        log.info("settlement.notify-complete paymentId={}", payment.getId());
        grpcClient.notifyComplete(payment);
    }

    public void notifySettlementFailed(Payment payment, String reason) {
        log.info("settlement.notify-failed paymentId={} reason={}", payment.getId(), reason);
        grpcClient.notifyFailed(payment, reason);
    }
}
