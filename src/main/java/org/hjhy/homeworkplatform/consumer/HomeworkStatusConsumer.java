package org.hjhy.homeworkplatform.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.ObjectStorageContext;
import org.hjhy.homeworkplatform.dto.HomeworkStatusDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.HomeworkSubmissionService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * @author HJHY
 * @date 2024/2/26 18:11
 */
@Component
@RabbitListener(queues = RabbitMQConfig.HOMEWORK_STATUS_QUEUE)
@Slf4j
public class HomeworkStatusConsumer {
    private final HomeworkSubmissionService homeworkSubmissionService;
    private final ObjectStorageContext objectStorageContext;

    public HomeworkStatusConsumer(HomeworkSubmissionService homeworkSubmissionService, ObjectStorageContext objectStorageContext) {
        this.homeworkSubmissionService = homeworkSubmissionService;
        this.objectStorageContext = objectStorageContext;
    }

    @RabbitHandler
    @Transactional
    public void process(HomeworkStatusDto homeworkStatusDto) {
        log.info("作业状态检查消费者收到消息:" + homeworkStatusDto);

        //检查作业是否已经提交
        var homeworkSubmission = homeworkSubmissionService.getById(homeworkStatusDto.getHomeworkId());
        if (ObjectUtils.isEmpty(homeworkSubmission)) {
            log.warn("作业id为{}的作业不存在", homeworkStatusDto.getHomeworkId());
            throw new BaseException(StatusCode.HOMEWORK_NOT_EXISTED);
        }
        //如果作业还已经提交则直接结束次消息(这里是自动ack,所以直接返回即可)
        if (homeworkSubmission.getStatus().equals(1)) {
            log.info("作业{}的状态为已提交", homeworkStatusDto);
            return;
        }

        //没有成功提交则到对象存储中查询文件是否存在
        if (objectStorageContext.exists(homeworkStatusDto.getFilePath())) {
            //文件存在则更新作业状态
            homeworkSubmission.setStatus(1);
            homeworkSubmissionService.updateById(homeworkSubmission);
            log.info("作业{}状态更新完成", homeworkStatusDto);
        }
    }
}
