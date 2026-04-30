package com.splitwise.notification.worker;

import com.rabbitmq.client.Channel;
import com.splitwise.notification.messaging.SplitwiseNotificationEventMessage;
import com.splitwise.notification.observability.NotificationMetrics;
import com.splitwise.notification.service.SplitwiseEventIngestionService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SplitwiseEventWorker {
    private final SplitwiseEventIngestionService splitwiseEventIngestionService;
    private final NotificationMetrics notificationMetrics;

    @RabbitListener(queues = "${notification.splitwise.queue}")
    public void consume(
            SplitwiseNotificationEventMessage message,
            Channel brokerChannel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            boolean processed = splitwiseEventIngestionService.ingest(message);
            if (processed) {
                log.info("Processed splitwise event eventId={} eventType={} recipientUserId={}",
                        message.eventId(), message.eventType(), message.recipientUserId());
            } else {
                log.info("Skipped duplicate splitwise event eventId={} recipientUserId={}",
                        message.eventId(), message.recipientUserId());
            }
            notificationMetrics.workerProcessed("splitwise_events");
            brokerChannel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to process splitwise event eventId={} recipientUserId={}",
                    message.eventId(), message.recipientUserId(), ex);
            notificationMetrics.workerFailed("splitwise_events");
            brokerChannel.basicNack(deliveryTag, false, false);
        }
    }
}
