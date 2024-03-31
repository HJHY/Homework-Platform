package org.hjhy.homeworkplatform.generator.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.constant.HomeworkConst;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.HomeworkPackagedFileCleanDto;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.dto.HomeworkStatusDto;
import org.hjhy.homeworkplatform.generator.service.MessageService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author HJHY
 * @date 2024/2/4 14:47
 */
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final RabbitTemplate rabbitTemplate;

    public MessageServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }


    @Override
    public void sendEmail(EmailDto emailDto) {
        String uuid = UUID.randomUUID().toString();
        //推送至消息队列发送
        rabbitTemplate.convertAndSend(RabbitMQConfig.NORMAL_DIRECT_EXCHANGE_NAME, RabbitMQConfig.EMAIL_QUEUE_ROUTING_KEY_NAME, emailDto, message -> {
            message.getMessageProperties().setCorrelationId(uuid);
            return message;
        }, new CorrelationData(uuid));
    }

    @Override
    public void checkHomeworkStatus(HomeworkStatusDto homeworkStatusDto) {
        String uuid = UUID.randomUUID().toString();
        //推送作业状态检查的延时任务至消息队列
        rabbitTemplate.convertAndSend(RabbitMQConfig.PLUGIN_DELAY_EXCHANGE_NAME,
                RabbitMQConfig.HOMEWORK_STATUS_QUEUE_ROUTING_KEY_NAME, homeworkStatusDto,
                message -> {
                    //延迟的实践设置为和上传凭证相同的实践
                    message.getMessageProperties().setHeader("x-delay", HomeworkConst.HOMEWORK_SUBMISSION_TOKEN_EXPIRE_TIME);
                    message.getMessageProperties().setCorrelationId(uuid);
                    return message;
                }, new CorrelationData(uuid));
        log.info("推送作业状态检查的延时任务至消息队列");
    }

    @Override
    public void cleanPackagedFile(HomeworkPackagedFileCleanDto homeworkPackagedFileCleanDto) {
        log.info("推送作业清理的延时任务至消息队列");
        String uuid = UUID.randomUUID().toString();
        //推送作业清理的延时任务至消息队列
        rabbitTemplate.convertAndSend(RabbitMQConfig.NORMAL_DIRECT_EXCHANGE_NAME,
                RabbitMQConfig.HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_ROUTING_KEY_NAME,
                homeworkPackagedFileCleanDto,
                message -> {
                    message.getMessageProperties().setCorrelationId(uuid);
                    return message;
                },
                new CorrelationData(uuid));
    }

    @Override
    public void sendHomeworkDelayReminderEmail(HomeworkReminderDto homeworkReminderDto, long delay) {
        String uuid = UUID.randomUUID().toString();
        //推送延时邮件至消息队列
        rabbitTemplate.convertAndSend(RabbitMQConfig.PLUGIN_DELAY_EXCHANGE_NAME,
                RabbitMQConfig.HOMEWORK_REMINDER_DELAY_ROUTING_KEY_NAME, homeworkReminderDto,
                message -> {
                    message.getMessageProperties().setHeader("x-delay", delay);
                    message.getMessageProperties().setCorrelationId(uuid);
                    return message;
                }, new CorrelationData(uuid));
    }
}
