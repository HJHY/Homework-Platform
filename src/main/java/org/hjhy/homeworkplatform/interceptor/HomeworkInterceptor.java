package org.hjhy.homeworkplatform.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.service.HomeworkReleaseService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author HJHY
 * @date 2024/2/29 20:30
 */
@Component
@Slf4j
public class HomeworkInterceptor implements HandlerInterceptor {
    private final HomeworkReleaseService homeworkReleaseService;

    public HomeworkInterceptor(HomeworkReleaseService homeworkReleaseService) {
        this.homeworkReleaseService = homeworkReleaseService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        //获取请求路径中的参数(pathParam)
        String path = request.getRequestURI(); // 获取请求的路径

        // 正则表达式匹配路径中的参数
        Pattern pattern = Pattern.compile("/homeworks/(\\d+)");
        Matcher matcher = pattern.matcher(path);

        if (!matcher.find()) {
            throw new BaseException("请求路径中未包含作业id");
        }

        String homeworkIdString = matcher.group(1); // 获取匹配的参数值
        int homeworkId = Integer.parseInt(homeworkIdString); // 将参数值转换为整数类型

        //查询作业对应的班级
        HomeworkRelease homeworkRelease = homeworkReleaseService.getCacheableHomework(homeworkId);
        if (ObjectUtils.isEmpty(homeworkRelease) || homeworkRelease.getIsValid().equals(0)) {
            log.warn("作业不存在或已被删除");
            throw new BaseException(StatusCode.HOMEWORK_NOT_EXISTED);
        }

        RequestContext.setHomeworkId(homeworkId);
        RequestContext.setClassId(homeworkRelease.getClassId());

        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) {
        RequestContext.removeClassId();
        RequestContext.removeHomeworkId();
    }
}
