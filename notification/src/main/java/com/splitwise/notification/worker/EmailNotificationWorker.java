package com.splitwise.notification.worker;

import com.rabbitmq.client.Channel;
import com.splitwise.notification.logging.LogContextKeys;
import com.splitwise.notification.logging.MdcScope;
import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.messaging.RetryPublisher;
import com.splitwise.notification.observability.NotificationMetrics;
import com.splitwise.notification.shared.CorrelationIdResolver;
import com.splitwise.notification.service.ChannelStateService;
import com.splitwise.notification.exception.custom.email.PermanentEmailException;
import com.splitwise.notification.exception.custom.email.TransientEmailException;
import com.splitwise.notification.service.email.EmailGateway;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationWorker.class);
    private final EmailGateway emailGateway;
    private final ChannelStateService channelStateService;
    private final RetryPublisher retryPublisher;
    private final NotificationMetrics notificationMetrics;
    private final CorrelationIdResolver correlationIdResolver;

    @RabbitListener(queues = "${notification.queue.email.main}")
    public void consume(
            NotificationMessage message,
            Channel brokerChannel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "X-Correlation-Id", required = false) String correlationId
    ) throws IOException {
        String effectiveCorrelationId = correlationIdResolver.resolve(correlationId, message);
        try (MdcScope ignored = MdcScope.with(Map.of(
                LogContextKeys.CORRELATION_ID, effectiveCorrelationId,
                LogContextKeys.NOTIFICATION_ID, message.notificationId().toString(),
                LogContextKeys.CHANNEL, message.channel().name(),
                LogContextKeys.EVENT_TYPE, message.eventType().name()
        ))) {
            try {
                emailGateway.send(message);
                markSent(message);
            } catch (TransientEmailException transientEmailException) {
                handleRetryableFailure(message, transientEmailException.getMessage(), "Transient email failure");
            } catch (PermanentEmailException permanentEmailException) {
                handlePermanentFailure(message, permanentEmailException.getMessage(), "Permanent email failure");
            } catch (Exception exception) {
                handleRetryableFailure(message, exception.getMessage(), "Unexpected email processing error");
            }
            brokerChannel.basicAck(deliveryTag, false);
        }
    }

    private void markSent(NotificationMessage message) {
        channelStateService.markSent(message.notificationId(), message.channel());
        LOGGER.info("Email sent successfully attempt={}", message.attempt());
        notificationMetrics.workerProcessed("email");
    }

    private void handleRetryableFailure(NotificationMessage message, String error, String logPrefix) {
        boolean queued = retryPublisher.publishRetry(message);
        if (queued) {
            long delay = retryPublisher.delaySecondsForAttempt(message.attempt());
            channelStateService.markRetrying(
                    message.notificationId(),
                    message.channel(),
                    error,
                    Instant.now().plusSeconds(delay)
            );
            LOGGER.warn("{} queued for retry attempt={} error={}", logPrefix, message.attempt(), error);
            notificationMetrics.workerRetried("email");
            return;
        }
        handlePermanentFailure(message, error, logPrefix + " exhausted retries");
    }

    private void handlePermanentFailure(NotificationMessage message, String error, String logPrefix) {
        channelStateService.markFailed(message.notificationId(), message.channel(), error);
        retryPublisher.publishDlq(message);
        LOGGER.error("{}; sent to DLQ error={}", logPrefix, error);
        notificationMetrics.workerFailed("email");
        notificationMetrics.workerDlqPublished("email");
    }
}
