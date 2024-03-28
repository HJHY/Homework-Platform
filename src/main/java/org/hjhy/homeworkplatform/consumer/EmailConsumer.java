package org.hjhy.homeworkplatform.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.utils.EmailUtils;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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

    public EmailConsumer(JavaMailSender javaMailSender, MailProperties mailProperties) {
        this.javaMailSender = javaMailSender;

        this.mailProperties = mailProperties;
    }

    @RabbitHandler
    public void process(EmailDto emailDto) {
        log.info("邮件发送消费者收到消息:" + emailDto);

        var mailMessage = new SimpleMailMessage();
        EmailUtils.sealMailMessage(mailMessage, emailDto);
        mailMessage.setFrom(mailProperties.getUsername());
        javaMailSender.send(mailMessage);

        log.info("邮件{}发送成功", emailDto);
    }
}
