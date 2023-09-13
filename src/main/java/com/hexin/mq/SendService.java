package com.hexin.mq;

import com.hexin.entity.EsMessage;
import com.hexin.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SendService {
    @Autowired
    private RabbitProductConfig rabbitProductConfig;

    private SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

    public void sendMessage(EsMessage msg, String queueName) {
        try {
            RabbitTemplate template = rabbitProductConfig.rabbitTemplate(queueName);
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            Message message = simpleMessageConverter.toMessage(JsonUtils.toJSONString(msg), messageProperties);
            template.send(message);
        } catch (Exception e) {

        }
    }
}
