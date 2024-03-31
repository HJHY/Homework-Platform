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

        try {
            ConsumedMessage consumedMessage = ConsumedMessage.builder().messageId(message.getMessageProperties().getCorrelationId()).build();
            log.info("保存消息{{}}", consumedMessage);
            consumedMessagesService.save(consumedMessage);

            //采用唯一性标识+消息去重等手段来保证消息不被重复消费
            var mailMessage = new SimpleMailMessage();
            EmailUtils.sealMailMessage(mailMessage, emailDto);
            mailMessage.setFrom(mailProperties.getUsername());
            javaMailSender.send(mailMessage);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (DuplicateKeyException e) {
            log.info("消息{{}}已经消费过", message.getMessageProperties());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        } catch (MailException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.warn("邮件{{}}发送失败", emailDto, e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        } catch (IOException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.error("消息{{}}ack失败,消息不重新入队", message.getMessageProperties());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        log.info("邮件{}发送成功", emailDto);
    }
}
