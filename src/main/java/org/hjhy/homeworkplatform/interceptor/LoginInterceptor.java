package org.hjhy.homeworkplatform.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.AuthConstant;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.AuthService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.hjhy.homeworkplatform.constant.StatusCode.USER_ALREADY_LOGIN;

/**
 * @author HJHY
 * @date 2024/2/7 12:18
 */
@Component
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        //获取cookie
        var cookie = CommonUtils.getCookie(request.getCookies(), AuthConstant.ACCESS_TOKEN_NAME);
        if (!ObjectUtils.isEmpty(cookie)) {
            //检查cookie中的token是否有效
            var authInfo = authService.getAuthInfoFromAccessToken(cookie.getValue());

            //判断是否已经登录(防止多次登录)
            if (!ObjectUtils.isEmpty(authInfo)) {
                log.info("用户{}已经处于登录状态,无需再次登录", authInfo.getUserId());
                throw new BaseException(USER_ALREADY_LOGIN);
            }
        }

        return true;
    }
}
