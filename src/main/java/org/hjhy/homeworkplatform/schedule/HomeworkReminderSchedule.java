package org.hjhy.homeworkplatform.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.generator.domain.HomeworkReminderMessage;
import org.hjhy.homeworkplatform.generator.service.HomeworkReminderMessageService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author HJHY
 * @date 2024/2/28 21:23
 */
@Component
@Slf4j
public class HomeworkReminderSchedule {
    private final HomeworkReminderMessageService homeworkReminderMessageService;
    private final RabbitTemplate rabbitTemplate;

    //创建一个线程池进行消息状态得异步更新
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public HomeworkReminderSchedule(HomeworkReminderMessageService homeworkReminderMessageService, RabbitTemplate rabbitTemplate) {
        this.homeworkReminderMessageService = homeworkReminderMessageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void scanHomeworkReminderInfo() {
        log.info("扫描当天需要推送的作业提醒消息....");
        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);
        List<HomeworkReminderMessage> messageRecordList = homeworkReminderMessageService.list(new LambdaQueryWrapper<HomeworkReminderMessage>()
                .between(HomeworkReminderMessage::getDealTime, startOfDay, endOfDay)
                .eq(HomeworkReminderMessage::getStatus, 0));
        //推送今天需要发送的消息到延时消息队列中
        for (HomeworkReminderMessage messageRecord : messageRecordList) {
            //状态为1表示已经推送过了无需重复消费
            if (messageRecord.getStatus().equals(1)) {
                continue;
            }

            Instant instant = messageRecord.getDealTime().toInstant();
            LocalDateTime dealDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            long delay = CommonUtils.getDelayTime(LocalDateTime.now(), dealDateTime);

            HomeworkReminderDto homeworkReminderDto = HomeworkReminderDto.builder()
                    .messageId(messageRecord.getId())
                    .userId(messageRecord.getUserId())
                    .homeworkId(messageRecord.getHomeworkId()).build();

            //推送延时邮件至消息队列
            String uuid = UUID.randomUUID().toString();
            rabbitTemplate.convertAndSend(RabbitMQConfig.PLUGIN_DELAY_EXCHANGE_NAME,
                    RabbitMQConfig.HOMEWORK_REMINDER_DELAY_ROUTING_KEY_NAME, homeworkReminderDto,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", delay);
                        message.getMessageProperties().setCorrelationId(uuid);
                        return message;
                    }, new CorrelationData(uuid));

            //更新消息状态并进行异步更新
            messageRecord.setLastUpdateTime(new Date());
            messageRecord.setStatus(1);
            executor.submit(() -> homeworkReminderMessageService.updateById(messageRecord));
        }
        log.info("扫描当天需要推送的作业提醒消息结束....");
    }
}
