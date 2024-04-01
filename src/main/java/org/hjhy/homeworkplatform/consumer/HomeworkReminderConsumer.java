package org.hjhy.homeworkplatform.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.constant.MessageConstant;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.HomeworkReminderMessage;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.service.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

/**
 * @author HJHY
 * @date 2024/2/28 21:00
 */
@Component
@RabbitListener(queues = RabbitMQConfig.HOMEWORK_REMINDER_DELAY_QUEUE_NAME)
@Slf4j
public class HomeworkReminderConsumer {
    private final MessageService messageService;
    private final HomeworkReminderMessageService homeworkReminderMessageService;
    private final UserService userService;
    private final ClazzService clazzService;
    private final HomeworkReleaseService homeworkReleaseService;
    private final UserClassRoleService userClassRoleService;

    public HomeworkReminderConsumer(MessageService messageService, HomeworkReminderMessageService homeworkReminderMessageService, UserService userService, ClazzService clazzService, HomeworkReleaseService homeworkReleaseService, UserClassRoleService userClassRoleService) {
        this.messageService = messageService;
        this.homeworkReminderMessageService = homeworkReminderMessageService;
        this.userService = userService;
        this.clazzService = clazzService;
        this.homeworkReleaseService = homeworkReleaseService;
        this.userClassRoleService = userClassRoleService;
    }

    @RabbitHandler
    public void process(HomeworkReminderDto homeworkReminderDto, Message message, Channel channel) throws IOException {
        log.info("作业截止提醒消费者收到消息:" + homeworkReminderDto);

        String correlationId = message.getMessageProperties().getCorrelationId();
        if (ObjectUtils.isEmpty(correlationId)) {
            log.error("消息{{}}没有correlationId", message.getMessageProperties());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        try {
            HomeworkRelease homeworkRelease = homeworkReleaseService.getById(homeworkReminderDto.getHomeworkId());
            if (ObjectUtils.isEmpty(homeworkRelease)) {
                log.error("作业信息不存在");
                return;
            }
            User user = userService.getById(homeworkReminderDto.getUserId());
            //进行作业信息推送的时候判断用户是否还在班级中
            if (userClassRoleService.getCachableUserClassRoleList(user.getId(), homeworkRelease.getClassId()).stream().noneMatch(userClassRole -> userClassRole.getRoleId().equals(RoleConstant.CLASS_MEMBER.getRoleId()))) {
                log.info("用户不在班级中,不推送作业截止提醒");
                return;
            }

            Clazz clazz = clazzService.getById(homeworkRelease.getClassId());

            String content = MessageConstant.DDL_MESSAGE.formatted(user.getRealname(), clazz.getClassName(), homeworkRelease.getHomeworkName(), homeworkRelease.getEndTime(), homeworkRelease.getDescription());
            EmailDto emailDto = EmailDto.builder().toEmail(user.getEmail()).subject("作业截止提醒").content(content).build();

            //推送延时消息
            messageService.sendEmail(emailDto);

            //这个数据是从数据库中扫描出来的,而不是是作业发布时用户设置的DDL计算后在当天出现的
            if (!ObjectUtils.isEmpty(homeworkReminderDto.getMessageId())) {
                //更新状态
                homeworkReminderMessageService.update(new LambdaUpdateWrapper<HomeworkReminderMessage>().set(HomeworkReminderMessage::getStatus, 1).eq(HomeworkReminderMessage::getId, homeworkReminderDto.getMessageId()));
            }

            log.info("作业提醒任务{}完成", homeworkReminderDto);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.error("消息{{}}ack失败,消息不重新入队", message.getMessageProperties());
            return;
        } catch (Exception e) {
            log.error("作业提醒任务{}失败,消息信息为{}", homeworkReminderDto, message.getMessageProperties());
        }
        //统一处理失败的情况(记录日志后直接丢弃消息)
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
