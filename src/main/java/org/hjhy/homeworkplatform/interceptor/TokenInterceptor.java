package org.hjhy.homeworkplatform.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.AuthConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.AuthService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 避免用户一直执行登录操作
 *
 * @author HJHY
 * @date 2024/2/6 11:36
 */
@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public TokenInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        //获取cookie
        var cookie = CommonUtils.getCookie(request.getCookies(), AuthConstant.ACCESS_TOKEN_NAME);
        if (ObjectUtils.isEmpty(cookie)) {
            log.warn("token不存在");
            throw new BaseException(StatusCode.NO_ACCESS_TOKEN);
        }
        //检查cookie中的token是否有效
        var authInfo = authService.getAuthInfoFromAccessToken(cookie.getValue());
        if (ObjectUtils.isEmpty(authInfo)) {
            log.warn("access token无效");
            throw new BaseException(StatusCode.INVALID_ACCESS_TOKEN);
        }

        //将用户信息保存到上下文
        RequestContext.setAuthInfo(authInfo);

        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        RequestContext.removeAuthInfo();
    }
}
