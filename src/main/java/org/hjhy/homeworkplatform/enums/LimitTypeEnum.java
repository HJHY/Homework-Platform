package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author HJHY
 * @date 2024/3/31 15:13
 */
@AllArgsConstructor
@Getter
public enum LimitTypeEnum {
    IP("ip"),
    USER_ID("userId"),
    //    ClASS_ID("classId"),
    GLOBAL("global");

    private final String typeName;
}
