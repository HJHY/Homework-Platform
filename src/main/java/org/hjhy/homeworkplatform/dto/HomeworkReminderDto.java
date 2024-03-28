package org.hjhy.homeworkplatform.dto;

import lombok.*;

/**
 * @author HJHY
 * @date 2024/2/28 21:12
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Builder
public class HomeworkReminderDto {
    /**
     * 消息id
     * 如果为null则说明消息不是从数据库里面扫描出来的,是在进行作业发布时用户设置的DDL计算后在当天出现的
     */
    private Integer messageId;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 作业id
     */
    private Integer homeworkId;
}
