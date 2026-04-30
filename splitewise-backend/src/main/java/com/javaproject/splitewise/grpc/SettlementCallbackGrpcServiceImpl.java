package com.javaproject.splitewise.grpc;

import com.javaproject.grpc.settlement.SettlementAck;
import com.javaproject.grpc.settlement.SettlementCallbackServiceGrpc;
import com.javaproject.grpc.settlement.SettlementCompleteEvent;
import com.javaproject.grpc.settlement.SettlementFailedEvent;
import com.javaproject.splitewise.dto.request.SettlementCompleteRequest;
import com.javaproject.splitewise.model.SagaTransaction;
import com.javaproject.splitewise.repository.SagaRepository;
import com.javaproject.splitewise.service.SettlementService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementCallbackGrpcServiceImpl
        extends SettlementCallbackServiceGrpc.SettlementCallbackServiceImplBase {

    private final SettlementService settlementService;
    private final SagaRepository sagaRepository;

    @Override
    public void notifySettlementComplete(SettlementCompleteEvent event,
                                         StreamObserver<SettlementAck> responseObserver) {
        log.info("grpc.callback.complete paymentId={} sagaId={} payer={} amount={}",
                event.getPaymentId(), event.getSagaId(), event.getPayerUserId(), event.getAmount());
        try {
            SettlementCompleteRequest req = new SettlementCompleteRequest(
                    UUID.fromString(event.getPaymentId()),
                    UUID.fromString(event.getPayerUserId()),
                    UUID.fromString(event.getPayeeUserId()),
                    event.getAmount(),
                    event.getCurrency()
            );
            settlementService.completeSettlement(req);

            updateSagaStatus(event.getSagaId(), "SETTLEMENT_CONFIRMED", null);

            responseObserver.onNext(SettlementAck.newBuilder().setReceived(true).setMessage("OK").build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("grpc.callback.complete.failed paymentId={} reason={}", event.getPaymentId(), ex.getMessage());
            responseObserver.onNext(SettlementAck.newBuilder().setReceived(false).setMessage(ex.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void notifySettlementFailed(SettlementFailedEvent event,
                                        StreamObserver<SettlementAck> responseObserver) {
        log.info("grpc.callback.failed paymentId={} sagaId={} reason={}",
                event.getPaymentId(), event.getSagaId(), event.getReason());
        try {
            updateSagaStatus(event.getSagaId(), "COMPENSATED", event.getReason());
            responseObserver.onNext(SettlementAck.newBuilder().setReceived(true).setMessage("OK").build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("grpc.callback.failed.error sagaId={} reason={}", event.getSagaId(), ex.getMessage());
            responseObserver.onNext(SettlementAck.newBuilder().setReceived(false).setMessage(ex.getMessage()).build());
            responseObserver.onCompleted();
        }
    }

    private void updateSagaStatus(String sagaIdStr, String status, String errorReason) {
        if (sagaIdStr == null || sagaIdStr.isBlank()) return;
        try {
            Optional<SagaTransaction> opt = sagaRepository.findById(UUID.fromString(sagaIdStr));
            opt.ifPresent(saga -> {
                saga.setStatus(status);
                if (errorReason != null) saga.setErrorReason(errorReason);
                saga.setUpdatedAt(OffsetDateTime.now());
                sagaRepository.save(saga);
            });
        } catch (Exception ex) {
            log.warn("grpc.callback.saga-update-failed sagaId={} reason={}", sagaIdStr, ex.getMessage());
        }
    }
}
