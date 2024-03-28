package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author HJHY
 * @date 2024/2/28 17:03
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public enum MessageRecordEnum {
    EMAIL(1, "邮箱"),

    WECHAT(2, "微信"),

    SMS(3, "短信"),

    QQ(4, "QQ");

    private Integer code;
    private String method;

}
