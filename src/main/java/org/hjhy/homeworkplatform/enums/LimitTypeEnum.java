package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 限流类型(用来控制限流的细粒度)
 * @author HJHY
 * @date 2024/3/31 15:13
 */
@AllArgsConstructor
@Getter
public enum LimitTypeEnum {
    IP("ip"),
    USER_ID("userId"),
    ClASS_ID("classId"),
    HOMEWORK_ID("homeworkId"),
    GLOBAL("global");

    private final String typeName;
}
