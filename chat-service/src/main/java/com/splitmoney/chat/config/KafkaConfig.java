package com.splitmoney.chat.config;

import com.splitmoney.chat.dto.ChatMessageEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConfig {

    @Value("${chat.kafka.topic:chat.messages}")
    private String chatTopic;

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name(chatTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
