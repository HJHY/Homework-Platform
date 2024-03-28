package org.hjhy.homeworkplatform.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式枚举
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {
    /**
     * 邮箱登录
     */
    EMAIL(1, "email", "邮箱登录", ""),
    /**
     * QQ登录
     */
    QQ(2, "qq", "QQ登录", "qqLoginStrategyImpl"),
    /**
     * 微博登录
     */
    WEIBO(3, "weibo", "微博登录", "weiboLoginStrategyImpl"),
    /**
     * Gitee登录
     */
    GITEE(4, "gitee", "Gitee登录", "giteeLoginStrategyImpl");

    /**
     * 登录方式代号
     */
    private final Integer type;

    /**
     * 模式
     */
    private final String mode;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 策略
     */
    private final String strategy;

    /**
     * 获取策略
     *
     * @param mode 登录模式
     * @return {@link String} 搜索策略
     */
    public static LoginTypeEnum getStrategyEnum(String mode) {
        for (LoginTypeEnum value : LoginTypeEnum.values()) {
            if (value.getMode().equals(mode)) {
                return value;
            }
        }
        return null;
    }
}
