package org.hjhy.homeworkplatform.annotation;

import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.enums.LimitTypeEnum;

import java.lang.annotation.*;

/**
 * @author HJHY
 * @date 2024/3/31 15:04
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RateLimiter {
    /**
     * 限流key
     */
    String key() default RedisPrefixConst.RATE_LIMIT_PREFIX;

    /**
     * 限流类型 ( 默认 Ip 模式 )
     */
    LimitTypeEnum limitType() default LimitTypeEnum.USER_ID;

    /**
     * 错误提示
     */
    StatusCode message() default StatusCode.REQUEST_FREQUENTLY;

    /**
     * 限流规则 （规则不可变，可多规则）
     */
    RateRule[] rules() default {};

    /**
     * 防重复提交值
     */
    boolean preventDuplicate() default false;

    /**
     * 防重复提交默认值
     */
    RateRule preventDuplicateRule() default @RateRule(count = 1, time = 5);
}
