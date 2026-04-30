package com.splitwise.notification.worker;

import com.rabbitmq.client.Channel;
import com.splitwise.notification.logging.LogContextKeys;
import com.splitwise.notification.logging.MdcScope;
import com.splitwise.notification.messaging.NotificationMessage;
import com.splitwise.notification.observability.NotificationMetrics;
import com.splitwise.notification.shared.CorrelationIdResolver;
import com.splitwise.notification.service.ChannelStateService;
import com.splitwise.notification.service.realtime.RealtimeNotifier;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(InAppNotificationWorker.class);
    private final ChannelStateService channelStateService;
    private final RealtimeNotifier realtimeNotifier;
    private final NotificationMetrics notificationMetrics;
    private final CorrelationIdResolver correlationIdResolver;

    public InAppNotificationWorker(
            ChannelStateService channelStateService,
            RealtimeNotifier realtimeNotifier,
            NotificationMetrics notificationMetrics,
            CorrelationIdResolver correlationIdResolver
    ) {
        this.channelStateService = channelStateService;
        this.realtimeNotifier = realtimeNotifier;
        this.notificationMetrics = notificationMetrics;
        this.correlationIdResolver = correlationIdResolver;
    }

    @RabbitListener(queues = "${notification.queue.in-app.main}")
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
            boolean updated = channelStateService.markSent(message.notificationId(), message.channel());
            if (updated) {
                realtimeNotifier.push(message);
            }
            LOGGER.info("Processed in-app notification attempt={}", message.attempt());
            notificationMetrics.workerProcessed("in_app");
            brokerChannel.basicAck(deliveryTag, false);
        }
    }
}
