package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【user_class_role(用户班级角色表)】的数据库操作Service
 * @createDate 2024-02-08 17:08:47
 */
public interface UserClassRoleService extends IService<UserClassRole> {

    /**
     * 获取班级内角色为学生的用户id列表
     *
     * @param classId classId
     * @return list
     */
    List<Integer> getStudentIdInClazz(Integer classId);

    /**
     * 检查用户是否有权限访问班级
     *
     * @param userId  userId
     * @param classId classId
     */
    void checkUserClassPrivilege(Integer userId, Integer classId, RoleConstant[] roles, HttpServletRequest request);

    /**
     * 获取用户在班级中的角色列表(先取缓存再取数据库)
     * 用户信息为空时,获取班级内所有用户的角色列表
     * 不允许classId为空
     *
     * @param userId  userId
     * @param classId classId
     * @return list
     */
    List<UserClassRole> getCachableUserClassRoleList(Integer userId, Integer classId);

    /**
     * 删除用户在班级中的角色
     * @param userId userId
     * @param classId classId
     */
    void deleteUserClassRole(Integer userId, Integer classId);

    /**
     * 删除用户在班级中的角色缓存
     * @param userId userId
     * @param classId classId
     */
    void invalidateUserClassRoleCache(Integer userId, Integer classId);
}
