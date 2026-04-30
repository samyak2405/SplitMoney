package com.splitmoney.application.grpc;

import com.javaproject.grpc.settlement.SettlementAck;
import com.javaproject.grpc.settlement.SettlementCallbackServiceGrpc;
import com.javaproject.grpc.settlement.SettlementCompleteEvent;
import com.javaproject.grpc.settlement.SettlementFailedEvent;
import com.splitmoney.application.domain.payment.Payment;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * gRPC client that notifies the splitwise-backend when a payment reaches
 * a terminal state. Replaces the previous HTTP SettlementNotifier.
 */
@Slf4j
@Component
public class SettlementCallbackGrpcClient {

    private final SettlementCallbackServiceGrpc.SettlementCallbackServiceBlockingStub stub;

    public SettlementCallbackGrpcClient(ManagedChannel settlementCallbackChannel) {
        this.stub = SettlementCallbackServiceGrpc.newBlockingStub(settlementCallbackChannel);
    }

    public void notifyComplete(Payment payment) {
        String sagaId = payment.getClientRequestId() != null ? payment.getClientRequestId() : "";
        try {
            SettlementCompleteEvent event = SettlementCompleteEvent.newBuilder()
                    .setPaymentId(payment.getId().toString())
                    .setSagaId(sagaId)
                    .setPayerUserId(payment.getPayerUserId().toString())
                    .setPayeeUserId(payment.getPayeeUserId().toString())
                    .setAmount(payment.getAmount().toPlainString())
                    .setCurrency(payment.getCurrency())
                    .build();

            SettlementAck ack = stub.notifySettlementComplete(event);
            log.info("grpc.settlement.complete.acked paymentId={} sagaId={} received={}",
                    payment.getId(), sagaId, ack.getReceived());
        } catch (Exception ex) {
            log.error("grpc.settlement.complete.failed paymentId={} sagaId={} reason={}",
                    payment.getId(), sagaId, ex.getMessage());
        }
    }

    public void notifyFailed(Payment payment, String reason) {
        String sagaId = payment.getClientRequestId() != null ? payment.getClientRequestId() : "";
        try {
            SettlementFailedEvent event = SettlementFailedEvent.newBuilder()
                    .setPaymentId(payment.getId().toString())
                    .setSagaId(sagaId)
                    .setReason(reason)
                    .build();

            SettlementAck ack = stub.notifySettlementFailed(event);
            log.info("grpc.settlement.failed.acked paymentId={} sagaId={} received={}",
                    payment.getId(), sagaId, ack.getReceived());
        } catch (Exception ex) {
            log.error("grpc.settlement.failed.send-failed paymentId={} sagaId={} reason={}",
                    payment.getId(), sagaId, ex.getMessage());
        }
    }
}
