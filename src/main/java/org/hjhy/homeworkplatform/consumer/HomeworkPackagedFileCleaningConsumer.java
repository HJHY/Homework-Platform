package org.hjhy.homeworkplatform.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.dto.HomeworkPackagedFileCleanDto;
import org.hjhy.homeworkplatform.strategy.ObjectStorageStrategy;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 作业打包文件清理任务的消费者
 *
 * @author HJHY
 * @date 2024/2/28 15:43
 */
@Component
@RabbitListener(queues = RabbitMQConfig.HOMEWORK_PACKAGED_FILE_CLEAN_QUEUE_NAME)
@Slf4j
public class HomeworkPackagedFileCleaningConsumer {

    private final ObjectStorageStrategy objectStorageStrategy;

    public HomeworkPackagedFileCleaningConsumer(ObjectStorageStrategy objectStorageStrategy) {
        this.objectStorageStrategy = objectStorageStrategy;
    }

    @RabbitHandler
    public void process(HomeworkPackagedFileCleanDto homeworkPackagedFileCleanDto, Message message, Channel channel) throws IOException {
        log.info("作业清理工作消费者收到消息" + homeworkPackagedFileCleanDto);

        boolean existsed = objectStorageStrategy.exists(homeworkPackagedFileCleanDto.getFilePath());
        if (!existsed) {
            log.warn("文件{}不存在,无需清理", homeworkPackagedFileCleanDto.getFilePath());
            return;
        }
        //删除oss文件
        objectStorageStrategy.deleteHomework(homeworkPackagedFileCleanDto.getFilePath());

        try {
            //这里即使重复消费也不会有影响
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            //这里ack失败直接记录日志不重新入队,避免消息重复失败打满日志
            log.error("消息{{}}ack失败,消息不重新入队", message.getMessageProperties());
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            return;
        }

        log.info("作业清理工作{}完成", homeworkPackagedFileCleanDto);
    }

}
