package com.hexin.mq;

import com.hexin.config.Config;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitProductConfig {

    @Autowired
    private Config config;

    public RabbitTemplate rabbitTemplate(String queue) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setRetryTemplate(retryTemplate());
        template.setDefaultReceiveQueue(queue);
        template.setRoutingKey(queue);
        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(10.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean(name = "productFactory")
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(config.getHost());
        connectionFactory.setUsername(config.getUsername());
        connectionFactory.setPassword(config.getPassword());
        return connectionFactory;
    }
}
