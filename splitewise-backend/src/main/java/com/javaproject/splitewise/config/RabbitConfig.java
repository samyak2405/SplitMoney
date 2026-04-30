package com.javaproject.splitewise.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    @Bean
    public Declarables userSyncTopology(UserSyncProperties props) {
        DirectExchange exchange = new DirectExchange(props.getExchange(), true, false);

        Queue dlq  = new Queue(props.getDlq(), true);
        Queue main = new Queue(props.getQueue(), true, false, false, Map.of(
                "x-dead-letter-exchange",    props.getExchange(),
                "x-dead-letter-routing-key", props.getDlq()
        ));

        Binding registeredBinding = BindingBuilder.bind(main).to(exchange).with("user.registered");
        Binding activatedBinding  = BindingBuilder.bind(main).to(exchange).with("user.activated");
        Binding dlqBinding        = BindingBuilder.bind(dlq).to(exchange).with(props.getDlq());

        return new Declarables(exchange, main, dlq, registeredBinding, activatedBinding, dlqBinding);
    }
}
