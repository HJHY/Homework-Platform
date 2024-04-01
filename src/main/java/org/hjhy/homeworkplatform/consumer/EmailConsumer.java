package org.hjhy.homeworkplatform.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.generator.domain.ConsumedMessage;
import org.hjhy.homeworkplatform.generator.service.ConsumedMessagesService;
import org.hjhy.homeworkplatform.utils.EmailUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

/**
 * @author HJHY
 * @date 2023/1/7 17:02
 */
@Component
@RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE_NAME)
@Slf4j
public class EmailConsumer {
    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final ConsumedMessagesService consumedMessagesService;

    public EmailConsumer(JavaMailSender javaMailSender, MailProperties mailProperties, ConsumedMessagesService consumedMessagesService) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.consumedMessagesService = consumedMessagesService;
    }

    @RabbitHandler
    @Transactional
    public void process(Message message, EmailDto emailDto, Channel channel) throws IOException {
        log.info("邮件发送消费者收到消息:" + emailDto);
        log.info("message:{}:", message.getMessageProperties());

        //判断消息序号的存在性,如果不存在则直接抛弃
        String correlationId = message.getMessageProperties().getCorrelationId();
        if (ObjectUtils.isEmpty(correlationId)) {
            log.error("消息{{}}没有correlationId", message.getMessageProperties());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        try {
            ConsumedMessage consumedMessage = ConsumedMessage.builder().messageId(correlationId).build();
            log.info("保存消息{{}}", consumedMessage);
            consumedMessagesService.save(consumedMessage);

            //采用唯一性标识+消息去重等手段来保证消息不被重复消费
            var mailMessage = new SimpleMailMessage();
            EmailUtils.sealMailMessage(mailMessage, emailDto);
            mailMessage.setFrom(mailProperties.getUsername());
            javaMailSender.send(mailMessage);
            log.info("邮件{}发送成功", emailDto);
            //确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        } catch (DuplicateKeyException e) {
            log.info("消息{{}}已经消费过", message.getMessageProperties());
        } catch (MailException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.warn("邮件{{}}发送失败", emailDto, e);
        } catch (IOException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.error("消息{{}}ack失败,消息不重新入队", message.getMessageProperties());
        } catch (Exception e) {
            log.error("消息{{}}处理失败", message.getMessageProperties(), e);
        }
        //统一处理失败的情况(记录日志后直接丢弃消息)
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
