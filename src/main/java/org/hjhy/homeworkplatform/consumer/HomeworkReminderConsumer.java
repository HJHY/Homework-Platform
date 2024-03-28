package org.hjhy.homeworkplatform.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.constant.MessageConstant;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.generator.domain.*;
import org.hjhy.homeworkplatform.generator.service.*;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

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
    public void process(HomeworkReminderDto homeworkReminderDto) {
        log.info("作业截止提醒消费者收到消息:" + homeworkReminderDto);

        HomeworkRelease homeworkRelease = homeworkReleaseService.getById(homeworkReminderDto.getHomeworkId());
        if (ObjectUtils.isEmpty(homeworkRelease)) {
            log.error("作业信息不存在");
            return;
        }
        User user = userService.getById(homeworkReminderDto.getUserId());
        //进行作业信息推送的时候判断用户是否还在班级中
        if (userClassRoleService.exists(new LambdaQueryWrapper<UserClassRole>()
                .eq(UserClassRole::getUserId, user.getId())
                .eq(UserClassRole::getClassId, homeworkRelease.getClassId())
                .eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER))) {
            log.info("用户不在班级中,不推送作业截止提醒");
            return;
        }

        Clazz clazz = clazzService.getById(homeworkRelease.getClassId());

        String content = MessageConstant.DDL_MESSAGE.formatted(
                user.getRealname(),
                clazz.getClassName(),
                homeworkRelease.getHomeworkName(),
                homeworkRelease.getEndTime(),
                homeworkRelease.getDescription());
        EmailDto emailDto = EmailDto.builder().toEmail(user.getEmail()).subject("作业截止提醒").content(content).build();

        //推送延时消息
        messageService.sendEmail(emailDto);

        //这个数据是从数据库中扫描出来的,而不是是作业发布时用户设置的DDL计算后在当天出现的
        if (!ObjectUtils.isEmpty(homeworkReminderDto.getMessageId())) {
            //更新状态
            homeworkReminderMessageService.update(new LambdaUpdateWrapper<HomeworkReminderMessage>()
                    .set(HomeworkReminderMessage::getStatus, 1)
                    .eq(HomeworkReminderMessage::getId, homeworkReminderDto.getMessageId()));
        }

        log.info("作业提醒任务{}完成", homeworkReminderDto);
    }
}
