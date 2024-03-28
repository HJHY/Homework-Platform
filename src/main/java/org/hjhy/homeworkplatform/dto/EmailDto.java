package org.hjhy.homeworkplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author HJHY
 * @date 2023/1/7 20:14
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EmailDto {
    /**
     * 邮件发送地址
     */
    private String fromEmail;

    /**
     * 目标地址
     */
    private String toEmail;

    /**
     * 主题
     */
    private String subject;

    /**
     * 邮件内容
     */
    private String content;

    /**
     * 描述信息
     */
    private String description;
}
