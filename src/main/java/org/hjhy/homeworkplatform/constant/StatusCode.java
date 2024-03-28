package org.hjhy.homeworkplatform.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回信息状态码常量
 * 数字'0'表示成功
 * 数字'-1'表示失败
 * 数字'1000'开头表示用户信息
 *
 * @author HJHY
 * @date 2023/1/6 23:01
 */
@Getter
@AllArgsConstructor
public enum StatusCode {

    /**
     * 成功统一代码
     */
    SUCCESS(0, "成功"),

    /**
     * 失败统一代码
     */
    FAIL(-1, "内部异常"),

    /**
     * 参数不合法
     */
    ILLEGAL_PARAMETER(1, "请求参数不合法"),

    /**
     * 资源不存在
     */
    RESOURCE_NOT_EXIST(2, "资源不存在"),

    /**
     * 没有权限
     */
    NO_PRIVILEGE(3, "没有权限"),

    /*----------------------------------------认证模块的异常状态码----------------------------------------*/

    /**
     * 注册验证码缺失
     */
    NO_REGISTER_CAPTCHA(1001, "注册验证码缺失"),

    /**
     * 注册验证码不匹配
     */
    REGISTER_CAPTCHA_NOT_MATCHED(1002, "注册验证码不匹配"),

    /**
     * token缺失或无效
     */
    INVALID_ACCESS_TOKEN(1003, "token无效"),

    /**
     * token无效
     */
    NO_ACCESS_TOKEN(1004, "token缺失"),

    /**
     * 登录失败
     */
    ACCESS_DENY(1005, "登录失败,请检查账号密码"),

    /**
     * refresh token与当前用户不匹配已经泄露
     */
    REFRESH_TOKEN_LEAKED(1006, "refresh token已泄露,请重新登录"),

    /**
     * refresh token不存在
     */
    REFRESH_TOKEN_NOT_EXIST(1007, "refresh token不存在"),

    /*----------------------------------------用户模块的异常状态码----------------------------------------*/
    /**
     * 用户不存在
     */
    USER_NOT_EXIST(2001, "用户不存在"),

    /**
     * 用户已经存在
     */
    USER_ALREADY_EXISTED(2002, "用户已存在"),

    /**
     * 密码错误
     */
    MISMATCH_PASSWORD(2003, "密码错误"),

    /**
     * 用户被封禁
     */
    USER_LOCKED(2004, "用户被封禁"),

    /**
     * 用户已登录
     */
    USER_ALREADY_LOGIN(2005, "用户已登录"),

    /**
     * 用户未设置密码
     */
    USER_PASSWORD_NOT_SEATED(2006, "用户未设置密码"),

    /*----------------------------------------班级模块的异常状态码----------------------------------------*/

    /**
     * 班级分享码
     */
    INVALID_CLASS_SHARE_CODE(3001, "分享码无效"),

    /**
     * 存在同名班级
     */
    DUPLICATED_CLASS_NAME(3002, "存在同名班级"),

    /**
     * 班级不存在
     */
    CLASS_NOT_EXISTED(3003, "班级不存在"),

    /**
     * 已加入班级
     */
    ALREADY_JOIN_CLASS(3004, "已加入班级"),

    /**
     * 没有加入班级
     */
    NOT_JOIN_CLASS(3005, "没有加入班级"),

    /**
     * 已退出班级
     */
    ALREADY_EXIT_CLASS(3006, "已退出班级"),

    /*----------------------------------------作业模块的异常状态码----------------------------------------*/

    /**
     * 没有提交的作业
     */
    NO_HOMEWORK_SUBMITTED(4001, "没有提交的作业"),

    /**
     * 作业文件打包中
     */
    HOMEWORK_PACKING_PROCESSING(4002, "文件打包中"),

    /**
     * 打包完成
     */
    HOMEWORK_PACKING_FINISHED(4003, "文件打包完成"),

    /**
     * 作业不存在
     */
    HOMEWORK_NOT_EXISTED(4004, "作业不存在");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 描述信息
     */
    private final String message;

}
