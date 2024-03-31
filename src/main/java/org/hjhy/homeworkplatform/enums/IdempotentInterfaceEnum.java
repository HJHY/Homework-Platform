package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 幂等性接口名称枚举
 * @author HJHY
 * @date 2024/3/30 21:43
 */
@Getter
@AllArgsConstructor
public enum IdempotentInterfaceEnum {
    HOMEWORK_SUBMIT("HomeworkSubmit");

    private final String interfaceName;
}
