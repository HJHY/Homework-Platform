package org.hjhy.homeworkplatform.context;

import org.hjhy.homeworkplatform.dto.AuthInfoDto;

/**
 * @author HJHY
 * @date 2024/2/6 14:37
 */
public class RequestContext {
    private static final ThreadLocal<AuthInfoDto> UserInfoThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Integer> classIdThreadLocal = new ThreadLocal<>();

    public static AuthInfoDto getAuthInfo() {
        return UserInfoThreadLocal.get();
    }

    public static void setAuthInfo(AuthInfoDto authInfoDto) {
        UserInfoThreadLocal.set(authInfoDto);
    }

    public static void removeAuthInfo() {
        UserInfoThreadLocal.remove();
    }

    public static Integer getClassId() {
        return classIdThreadLocal.get();
    }

    public static void setClassId(Integer classId) {
        classIdThreadLocal.set(classId);
    }

    public static void removeClassId() {
        classIdThreadLocal.remove();
    }
}
