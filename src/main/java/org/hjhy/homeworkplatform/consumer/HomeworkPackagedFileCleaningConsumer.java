package org.hjhy.homeworkplatform.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.RabbitMQConfig;
import org.hjhy.homeworkplatform.dto.HomeworkPackagedFileCleanDto;
import org.hjhy.homeworkplatform.strategy.ObjectStorageStrategy;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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
    public void process(HomeworkPackagedFileCleanDto homeworkPackagedFileCleanDto) {
        log.info("作业清理工作消费者收到消息" + homeworkPackagedFileCleanDto);

        boolean existsed = objectStorageStrategy.exists(homeworkPackagedFileCleanDto.getFilePath());
        if (!existsed) {
            log.warn("文件{}不存在,无需清理", homeworkPackagedFileCleanDto.getFilePath());
            return;
        }
        //删除oss文件
        objectStorageStrategy.deleteHomework(homeworkPackagedFileCleanDto.getFilePath());

        log.info("作业清理工作{}完成", homeworkPackagedFileCleanDto);
    }

}
