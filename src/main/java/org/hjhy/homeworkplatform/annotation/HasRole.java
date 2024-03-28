package org.hjhy.homeworkplatform.annotation;

import org.hjhy.homeworkplatform.constant.RoleConstant;

import java.lang.annotation.*;

/**
 * 最低的访问权限要求
 *
 * @author HJHY
 * @date 2023/7/8 14:15
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasRole {
    RoleConstant[] roles() default {RoleConstant.CLASS_MEMBER, RoleConstant.CLASS_ADMIN, RoleConstant.CLASS_CREATOR};
}
