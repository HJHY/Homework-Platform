package org.hjhy.homeworkplatform.config;

import org.hjhy.homeworkplatform.constant.HomeworkConst;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HJHY
 * @date 2022/3/26 - 16:17
 */
@Configuration
public class RabbitMQConfig {
    /**
     * 普通直连交换机的名称
     */
    public static final String NORMAL_DIRECT_EXCHANGE_NAME = "normal_direct_exchange";

    /**
     * 插件延时交换机名称
     */
    public static final String PLUGIN_DELAY_EXCHANGE_NAME = "PLUGIN_DELAY_EXCHANGE";

    /**
     * 死信交换机名称(消息死了之后会发送到这个交换机)
     */
    public static final String NORMAL_DEAD_EXCHANGE_NAME = "normal_dead_exchange";
    /**
     * 作业提交状态检查的队列名称
     */
    public static final String HOMEWORK_STATUS_QUEUE = "homework_status_queue";
    /**
     * 作业提交检查状态的Routing Key名称
     */
    public static final String HOMEWORK_STATUS_QUEUE_ROUTING_KEY_NAME = "homework_status_queue_routing_key";
    /**
     * 注册验证码发送功能的队列名称
     */
    public static final String EMAIL_QUEUE_NAME = "email_queue";
    /**
     * 注册验证码发送功能的Routing Key名称
     */
    public static final String EMAIL_QUEUE_ROUTING_KEY_NAME = "email_queue_routing_key";
    /**
     * 作业打包文件清理任务消息延迟队列
     */
    public static final String HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_QUEUE = "homework_packaged_file_clean_delay_queue";

    /*------------------------------------用户模块进行消息队列的配置(采用hello word模式)------------------------------------*/
    /**
     * 作业打包文件清理任务消息延迟队列路由
     */
    public static final String HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_ROUTING_KEY_NAME = "homework_packaged_file_clean_delay_routing_key";
    /**
     * 作业打包文件清理任务消息队列
     */
    public static final String HOMEWORK_PACKAGED_FILE_CLEAN_QUEUE_NAME = "homework_packaged_file_clean_queue";
    /*
     *作业打包文件清理任务消息路由
     */
    public static final String HOMEWORK_PACKAGED_FILE_CLEAN_ROUTING_KEY_NAME = "homework_packaged_file_clean_routing_key";

    /*------------------------------------作业模块进行消息队列的配置(采用hello word模式)------------------------------------*/
    //作业延迟提醒通知队列
    public static final String HOMEWORK_REMINDER_DELAY_QUEUE_NAME = "homework_reminder_delay_queue";
    //作业延迟提醒通知队列路由
    public static final String HOMEWORK_REMINDER_DELAY_ROUTING_KEY_NAME = "homework_reminder_delay_routing_key";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean(NORMAL_DIRECT_EXCHANGE_NAME)
    public DirectExchange getNormalDirectExchange() {
        return new DirectExchange(NORMAL_DIRECT_EXCHANGE_NAME, true, false);
    }

    @Bean(name = EMAIL_QUEUE_NAME)
    public Queue getEmailQueue() {
        return new Queue(EMAIL_QUEUE_NAME, true);
    }

    @Bean(name = HOMEWORK_STATUS_QUEUE)
    public Queue getHomeworkStatusQueue() {
        return new Queue(HOMEWORK_STATUS_QUEUE, true);
    }

    @Bean(NORMAL_DEAD_EXCHANGE_NAME)
    public DirectExchange getNormalDeadExchange() {
        return new DirectExchange(NORMAL_DEAD_EXCHANGE_NAME, true, false);
    }

    @Bean(name = PLUGIN_DELAY_EXCHANGE_NAME)
    public CustomExchange getCustomExchange() {
        Map<String, Object> args = new HashMap<>(1);
        args.put("x-delayed-type", "direct");
        return new CustomExchange(PLUGIN_DELAY_EXCHANGE_NAME, "x-delayed-message", true, false, args);
    }

    @Bean
    public Binding bindEmailQueue(@Qualifier(NORMAL_DIRECT_EXCHANGE_NAME) DirectExchange normalDirectExchange, @Qualifier(EMAIL_QUEUE_NAME) Queue emailQueue) {
        return BindingBuilder.bind(emailQueue).to(normalDirectExchange).with(EMAIL_QUEUE_ROUTING_KEY_NAME);
    }

    @Bean
    public Binding bindHomeworkStatusQueue(@Qualifier(PLUGIN_DELAY_EXCHANGE_NAME) CustomExchange delayExchange, @Qualifier(HOMEWORK_STATUS_QUEUE) Queue homeworkStatusQueue) {
        return BindingBuilder.bind(homeworkStatusQueue).to(delayExchange).with(HOMEWORK_STATUS_QUEUE_ROUTING_KEY_NAME).noargs();
    }

    /**
     * 作业打包文件清理任务的延迟队列
     *
     * @return queue
     */
    @Bean(name = HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_QUEUE)
    public Queue getHomeworkPackagedFileCleanDelayQueue() {
        HashMap<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", NORMAL_DEAD_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", HOMEWORK_PACKAGED_FILE_CLEAN_ROUTING_KEY_NAME);
        args.put("x-message-ttl", HomeworkConst.HOMEWORK_DOWNLOAD_TOKEN_EXPIRE_TIME);
        return QueueBuilder.durable(HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding bindHomeworkPackagedFileCleanDelayQueue(@Qualifier(NORMAL_DIRECT_EXCHANGE_NAME) DirectExchange normalDirectExchange, @Qualifier(HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_QUEUE) Queue homeworkPackagedFileCleanDelayQueue) {
        return BindingBuilder.bind(homeworkPackagedFileCleanDelayQueue).to(normalDirectExchange).with(HOMEWORK_PACKAGED_FILE_CLEAN_DELAY_ROUTING_KEY_NAME);
    }

    /**
     * 作业打包文件清理任务的处理队列
     */
    @Bean(name = HOMEWORK_PACKAGED_FILE_CLEAN_QUEUE_NAME)
    public Queue getHomeworkPackagedFileCleanQueue() {
        return new Queue(HOMEWORK_PACKAGED_FILE_CLEAN_QUEUE_NAME, true);
    }

    @Bean
    public Binding bindHomeworkPackagedFileCleanQueue(@Qualifier(NORMAL_DEAD_EXCHANGE_NAME) DirectExchange normalDeadExchange, @Qualifier(HOMEWORK_PACKAGED_FILE_CLEAN_QUEUE_NAME) Queue homeworkPackagedFileCleanQueue) {
        return BindingBuilder.bind(homeworkPackagedFileCleanQueue).to(normalDeadExchange).with(HOMEWORK_PACKAGED_FILE_CLEAN_ROUTING_KEY_NAME);
    }

    @Bean(HOMEWORK_REMINDER_DELAY_QUEUE_NAME)
    public Queue homeworkReminderDelayQueue() {
        return new Queue(HOMEWORK_REMINDER_DELAY_QUEUE_NAME, true);
    }

    @Bean
    public Binding bindHomeworkReminderDelayQueue(@Qualifier(PLUGIN_DELAY_EXCHANGE_NAME) CustomExchange delayExchange, @Qualifier(HOMEWORK_REMINDER_DELAY_QUEUE_NAME) Queue homeworkReminderDelayQueue) {
        return BindingBuilder.bind(homeworkReminderDelayQueue).to(delayExchange).with(HOMEWORK_REMINDER_DELAY_ROUTING_KEY_NAME).noargs();
    }
}
