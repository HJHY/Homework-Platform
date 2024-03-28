package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author HJHY
 * @date 2024/2/29 22:07
 */
@Getter
@AllArgsConstructor
public enum ObjectStorageModeEnum {
    /**
     * oss
     */
    OSS("oss", "ossStrategyImpl"),
    /**
     * 本地
     */
    LOCAL("local", "localStrategyImpl"),

    /**
     * cos
     */
    COS("cos", "cosStrategyImpl");

    /**
     * 模式
     */
    private final String mode;

    /**
     * 策略
     */
    private final String strategy;

    /**
     * 获取策略
     *
     * @param mode 模式
     * @return {@link String} 搜索策略
     */
    public static String getStrategy(String mode) {
        for (ObjectStorageModeEnum value : ObjectStorageModeEnum.values()) {
            if (value.getMode().equals(mode)) {
                return value.getStrategy();
            }
        }
        return null;
    }
}
