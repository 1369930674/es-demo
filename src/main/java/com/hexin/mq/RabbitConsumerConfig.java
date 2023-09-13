package com.hexin.mq;

import com.hexin.config.Config;
import com.hexin.entity.Constant;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@Component
public class RabbitConsumerConfig {

    @Autowired
    private Config config;

    @Autowired
    @Qualifier("stringMessageHandler")
    private StringMessageHandler listener;

    @Bean(name = "listenerContainer")
    public SimpleMessageListenerContainer listenerContainer() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        rabbitAdmin.declareQueue(hexinQueue(Constant.QUEUE_NAME));
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setQueueNames(Constant.QUEUE_NAME);
        container.setAmqpAdmin(rabbitAdmin);
        container.setMessageListener(listener);
        container.setConcurrentConsumers(config.getConcurrentConsumers());
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return container;
    }


    public Queue hexinQueue(String name) {
        //持久化、共享的、不自动删除的Queue
        return new Queue(name, true, false, false);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        //常见与rabbitmq的连接
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(config.getHost());
        connectionFactory.setUsername(config.getUsername());
        connectionFactory.setPassword(config.getPassword());
        connectionFactory.setCloseTimeout(20000);
        Connection connection = connectionFactory.createConnection();
        connection.createChannel(true);
        return connectionFactory;
    }
}