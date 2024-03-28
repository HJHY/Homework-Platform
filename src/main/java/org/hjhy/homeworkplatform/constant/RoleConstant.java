package org.hjhy.homeworkplatform.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author HJHY
 * @date 2023/7/6 15:39
 */
@Getter
@AllArgsConstructor
public enum RoleConstant {
    SUPER_ADMIN(1, "超级管理员", "系统最高级管理员"),

    CLASS_CREATOR(2, "班级创建者", "班级创建者拥有该班级的所有权限"),

    CLASS_ADMIN(3, "班级管理员", "班级管理员:班级成员中的具有管理权限的特殊成员"),

    CLASS_MEMBER(4, "班级成员", "班级普通成员");

    private final Integer roleId;
    private final String roleName;
    private final String roleLabel;
}
