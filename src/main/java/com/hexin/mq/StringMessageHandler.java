package com.hexin.mq;

import com.hexin.entity.Constant;
import com.hexin.entity.EsMessage;
import com.hexin.service.AudioService;
import com.hexin.service.EsService;
import com.hexin.util.json.JsonUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service("stringMessageHandler")
@Slf4j
public class StringMessageHandler extends MessageListenerAdapter {
    @Autowired
    private AudioService audioService;
    private SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        try {
            String msg = (String) simpleMessageConverter.fromMessage(message);
            log.info("mq receive msg: {}", msg);
            EsMessage esMessage = JsonUtils.toJavaObject(msg, EsMessage.class);
            String infoType = esMessage.getInfoType();
            Integer fileId = esMessage.getFileId();
            switch (infoType) {
                case Constant.MQ_INSERT:
                case Constant.MQ_UPDATE:
                    audioService.syncAudioInfoFromSqlToEs(fileId);
                    break;
                case Constant.MQ_DELETE:
                    audioService.deleteAudioInfo(fileId);
                    break;
                default:
                    break;
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
