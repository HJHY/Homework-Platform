package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.generator.mapper.UserClassRoleMapper;
import org.hjhy.homeworkplatform.generator.service.ClazzService;
import org.hjhy.homeworkplatform.generator.service.UserClassRoleService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;

/**
 * @author 13746
 * @description 针对表【user_class_role(用户班级角色表)】的数据库操作Service实现
 * @createDate 2024-02-08 17:08:47
 */
@Service
@Slf4j
public class UserClassRoleServiceImpl extends ServiceImpl<UserClassRoleMapper, UserClassRole>
        implements UserClassRoleService {
    private final ClazzService clazzService;

    public UserClassRoleServiceImpl(@Lazy ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    @Override
    public List<Integer> getStudentIdInClazz(Integer classId) {
        List<UserClassRole> userClassRoleList = this.list(new LambdaQueryWrapper<UserClassRole>()
                .select(UserClassRole::getUserId)
                .eq(UserClassRole::getClassId, classId)
                .eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER.getRoleId())
                .eq(UserClassRole::getIsValid, 1));
        return userClassRoleList.stream().map(UserClassRole::getUserId).toList();
    }

    @Override
    public void checkUserClassPrivilege(Integer userId, Integer classId, RoleConstant[] roles, HttpServletRequest request) {
        //检查班级是否存在
        var existed = clazzService.exists(new LambdaQueryWrapper<Clazz>().eq(Clazz::getClassId, classId).eq(Clazz::getIsValid, 1));
        if (!existed) {
            log.error("classId为{{}}的班级不存在", classId);
            throw new BaseException(StatusCode.CLASS_NOT_EXISTED);
        }

        //检查用户在班级中的角色(一个用户可能存在多个角色)
        var userClassRoleList = this.list(new LambdaQueryWrapper<UserClassRole>()
                        .select(UserClassRole::getRoleId)
                        .eq(UserClassRole::getUserId, userId)
                        .eq(UserClassRole::getClassId, classId))
                .stream().map(UserClassRole::getRoleId).toList();
        if (ObjectUtils.isEmpty(userClassRoleList)) {
            log.error("当前用户{{}}没有加入id为{{}}的班级", userId, classId);
            throw new BaseException(StatusCode.NOT_JOIN_CLASS);
        }

        //传递过来的request参数为null,说明是前置切面没法检查权限但是业务中又需要检查权限,直接放行即可
        if (ObjectUtils.isEmpty(request)) {
            return;
        }

        //检查用户权限
        if (Arrays.stream(roles).noneMatch(roleConstant -> userClassRoleList.contains(roleConstant.getRoleId()))) {
            log.error("用户{{}}没有访问{{}}的权限", userId, request.getRequestURI());
            throw new BaseException(StatusCode.NO_PRIVILEGE);
        }
    }
}




