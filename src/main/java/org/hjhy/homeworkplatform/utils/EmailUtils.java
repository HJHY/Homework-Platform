package org.hjhy.homeworkplatform.utils;

import org.hjhy.homeworkplatform.dto.EmailDto;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author HJHY
 * @date 2024/2/7 11:37
 */
public class EmailUtils {
    /**
     * 对emailDto进行SimpleMailMessage的封装
     *
     * @param mailMessage message
     * @param emailDto    emailDto
     */
    public static void sealMailMessage(SimpleMailMessage mailMessage, EmailDto emailDto) {
        mailMessage.setFrom(emailDto.getFromEmail());
        mailMessage.setTo(emailDto.getToEmail());
        mailMessage.setSubject(emailDto.getSubject());
        mailMessage.setText(emailDto.getContent());
    }
}
