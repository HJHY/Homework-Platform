package org.hjhy.homeworkplatform.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author HJHY
 * @date 2024/3/31 15:04
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RateRule {
    /**
     * 限流次数
     */
    long count() default 10;

    /**
     * 限流时间(s)
     */
    long time() default 60;

    /**
     * 限流时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
