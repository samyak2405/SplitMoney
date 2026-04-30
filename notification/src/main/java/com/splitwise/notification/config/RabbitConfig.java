package com.splitwise.notification.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    Declarables notificationTopology(NotificationQueueProperties queueProperties, NotificationRoutingProperties routingProperties) {
        DirectExchange exchange = new DirectExchange(queueProperties.getExchange(), true, false);

        Queue inAppMain = new Queue(queueProperties.getInApp().getMain(), true);
        Queue inAppDlq = new Queue(queueProperties.getInApp().getDlq(), true);
        Queue inAppRetry1 = retryQueue(queueProperties.getInApp().getRetry1m(), queueProperties, routingProperties.getInApp(), 60_000);
        Queue inAppRetry5 = retryQueue(queueProperties.getInApp().getRetry5m(), queueProperties, routingProperties.getInApp(), 300_000);
        Queue inAppRetry15 = retryQueue(queueProperties.getInApp().getRetry15m(), queueProperties, routingProperties.getInApp(), 900_000);
        Queue inAppRetry60 = retryQueue(queueProperties.getInApp().getRetry60m(), queueProperties, routingProperties.getInApp(), 3_600_000);

        Queue emailMain = new Queue(queueProperties.getEmail().getMain(), true);
        Queue emailDlq = new Queue(queueProperties.getEmail().getDlq(), true);
        Queue emailRetry1 = retryQueue(queueProperties.getEmail().getRetry1m(), queueProperties, routingProperties.getEmail(), 60_000);
        Queue emailRetry5 = retryQueue(queueProperties.getEmail().getRetry5m(), queueProperties, routingProperties.getEmail(), 300_000);
        Queue emailRetry15 = retryQueue(queueProperties.getEmail().getRetry15m(), queueProperties, routingProperties.getEmail(), 900_000);
        Queue emailRetry60 = retryQueue(queueProperties.getEmail().getRetry60m(), queueProperties, routingProperties.getEmail(), 3_600_000);

        return new Declarables(
                exchange,
                inAppMain, inAppDlq, inAppRetry1, inAppRetry5, inAppRetry15, inAppRetry60,
                emailMain, emailDlq, emailRetry1, emailRetry5, emailRetry15, emailRetry60,
                bind(exchange, inAppMain, routingProperties.getInApp()),
                bind(exchange, inAppDlq, queueProperties.getInApp().getDlq()),
                bind(exchange, inAppRetry1, queueProperties.getInApp().getRetry1m()),
                bind(exchange, inAppRetry5, queueProperties.getInApp().getRetry5m()),
                bind(exchange, inAppRetry15, queueProperties.getInApp().getRetry15m()),
                bind(exchange, inAppRetry60, queueProperties.getInApp().getRetry60m()),
                bind(exchange, emailMain, routingProperties.getEmail()),
                bind(exchange, emailDlq, queueProperties.getEmail().getDlq()),
                bind(exchange, emailRetry1, queueProperties.getEmail().getRetry1m()),
                bind(exchange, emailRetry5, queueProperties.getEmail().getRetry5m()),
                bind(exchange, emailRetry15, queueProperties.getEmail().getRetry15m()),
                bind(exchange, emailRetry60, queueProperties.getEmail().getRetry60m())
        );
    }

    @Bean
    Declarables splitwiseTopology(SplitwiseEventProperties splitwiseEventProperties) {
        DirectExchange exchange = new DirectExchange(splitwiseEventProperties.getExchange(), true, false);
        Queue splitwiseMain = new Queue(splitwiseEventProperties.getQueue(), true, false, false, Map.of(
                "x-dead-letter-exchange", splitwiseEventProperties.getExchange(),
                "x-dead-letter-routing-key", splitwiseEventProperties.getDlq()
        ));
        Queue splitwiseDlq = new Queue(splitwiseEventProperties.getDlq(), true);

        return new Declarables(
                exchange,
                splitwiseMain,
                splitwiseDlq,
                bind(exchange, splitwiseMain, splitwiseEventProperties.getRoutingKeyGroupMemberAdded()),
                bind(exchange, splitwiseMain, splitwiseEventProperties.getRoutingKeyGroupMemberRemoved()),
                bind(exchange, splitwiseMain, splitwiseEventProperties.getRoutingKeyExpenseAddedAgainstUser()),
                bind(exchange, splitwiseDlq, splitwiseEventProperties.getDlq())
        );
    }

    private Binding bind(DirectExchange exchange, Queue queue, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    private Queue retryQueue(String name, NotificationQueueProperties queueProperties, String returnRoutingKey, int ttlMs) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", ttlMs);
        args.put("x-dead-letter-exchange", queueProperties.getExchange());
        args.put("x-dead-letter-routing-key", returnRoutingKey);
        return new Queue(name, true, false, false, args);
    }
}
