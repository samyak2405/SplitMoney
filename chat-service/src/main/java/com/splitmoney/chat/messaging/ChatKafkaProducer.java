package com.splitmoney.chat.messaging;

import com.splitmoney.chat.dto.ChatMessageEvent;
import com.splitmoney.chat.observability.ChatMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatKafkaProducer {

    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    private final String topic;
    private final ChatMetrics chatMetrics;

    public ChatKafkaProducer(
            KafkaTemplate<String, ChatMessageEvent> kafkaTemplate,
            @Value("${chat.kafka.topic:chat.messages}") String topic,
            ChatMetrics chatMetrics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.chatMetrics = chatMetrics;
    }

    public void publish(ChatMessageEvent event) {
        // key = groupId → guarantees ordering within a group on same partition
        kafkaTemplate.send(topic, event.getGroupId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        chatMetrics.recordKafkaPublishFailed();
                    }
                });
    }
}
