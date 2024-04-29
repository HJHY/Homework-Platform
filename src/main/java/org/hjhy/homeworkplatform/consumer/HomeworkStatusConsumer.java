package org.hjhy.homeworkplatform.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.ObjectStorageContext;
import org.hjhy.homeworkplatform.dto.HomeworkStatusDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.HomeworkSubmissionService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

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
    @Transactional(rollbackFor = Exception.class)
    public void process(HomeworkStatusDto homeworkStatusDto, Message message, Channel channel) throws IOException {
        log.info("作业状态检查消费者收到消息:{}", homeworkStatusDto);

        String correlationId = message.getMessageProperties().getCorrelationId();
        if (ObjectUtils.isEmpty(correlationId)) {
            log.error("消息{{}}没有correlationId", message.getMessageProperties());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        try {
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

            //通过业务的幂等性处理可以避免重复消费的影响
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        } catch (IOException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.error("消息{{}}ack失败,消息不重新入队", message.getMessageProperties());
        } catch (Exception e) {
            log.error("作业状态检查{}失败,消息信息为{}", homeworkStatusDto, message.getMessageProperties());
        }
        //统一处理失败的情况(记录日志后直接丢弃消息)
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
