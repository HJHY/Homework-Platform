package org.hjhy.homeworkplatform.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.hjhy.homeworkplatform.annotation.HasRole;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.generator.service.UserClassRoleService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 进行班级接口的权限检查
 *
 * @author yezhiqiu
 * @date 2021/07/27
 */
@Aspect
@Component
@Slf4j
@Order(1)
public class ClassAspect {
    private final UserClassRoleService userClassRoleService;

    public ClassAspect(UserClassRoleService userClassRoleService) {
        this.userClassRoleService = userClassRoleService;
    }

    /**
     * 检查当前用户是否可以访问当前班级的资源
     */
    @Before("@annotation(org.hjhy.homeworkplatform.annotation.HasRole)")
    public void before(JoinPoint joinPoint) {
        log.info("进行班级接口的权限检查");
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new RuntimeException("权限检查的切面获取request产生空指针");
        }

        //转换很重要!!
        var signature = (MethodSignature) joinPoint.getSignature();

        var method = signature.getMethod();
        // 获取目标方法上的@HasRole注解
        HasRole annotation = method.getAnnotation(HasRole.class);

        //注解没有添加在方法上直接返回不需要检查
        if (ObjectUtils.isEmpty(annotation)) {
            return;
        }

        var request = requestAttributes.getRequest();
        //获取用户id
        var userId = RequestContext.getAuthInfo().getUserId();
        //获取班级id
        var classId = RequestContext.getClassId();

        //检查用户是否有权限访问班级
        userClassRoleService.checkUserClassPrivilege(userId, classId, annotation.roles(), request);

        log.info("用户权限检查通过");
    }
}