package org.hjhy.homeworkplatform.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author HJHY
 * @date 2024/2/19 20:12
 */
@Component
@Slf4j
public class ClassInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        //获取请求路径中的参数(pathParam)
        String path = request.getRequestURI(); // 获取请求的路径

        // 正则表达式匹配路径中的参数
        Pattern pattern = Pattern.compile("/classes/(\\d+)");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            String classIdString = matcher.group(1); // 获取匹配的参数值
            int classId = Integer.parseInt(classIdString); // 将参数值转换为整数类型
            RequestContext.setClassId(classId);
        } else {
            throw new BaseException("请求路径中未包含班级id");
        }

        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        RequestContext.removeClassId();
    }
}
