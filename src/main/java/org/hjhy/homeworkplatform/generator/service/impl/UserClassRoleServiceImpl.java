package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.ClazzConst;
import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.generator.mapper.UserClassRoleMapper;
import org.hjhy.homeworkplatform.generator.service.ClazzService;
import org.hjhy.homeworkplatform.generator.service.UserClassRoleService;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    public UserClassRoleServiceImpl(@Lazy ClazzService clazzService, RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient) {
        this.clazzService = clazzService;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
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
        //使用缓存检查班级是否存在
        Clazz clazz = clazzService.getCacheableClazz(classId);
        if (ObjectUtils.isEmpty(clazz)) {
            log.error("classId为{{}}的班级不存在", classId);
            throw new BaseException(StatusCode.CLASS_NOT_EXISTED);
        }

        //检查用户在班级中的角色(一个用户可能存在多个角色)
        List<UserClassRole> userClassRoleList = getCacheableUserClassRoleList(userId, classId);
        if (ObjectUtils.isEmpty(userClassRoleList)) {
            log.error("当前用户{{}}没有加入id为{{}}的班级", userId, classId);
            throw new BaseException(StatusCode.NOT_JOIN_CLASS);
        }

        //传递过来的request参数为null,说明是前置切面没法检查权限但是业务中又需要检查权限,直接放行即可
        if (ObjectUtils.isEmpty(request)) {
            return;
        }

        //检查用户权限
        List<Integer> userClassRoles = userClassRoleList.stream().map(UserClassRole::getRoleId).toList();
        if (Arrays.stream(roles).noneMatch(roleConstant -> userClassRoles.contains(roleConstant.getRoleId()))) {
            log.error("用户{{}}没有访问{{}}的权限", userId, request.getRequestURI());
            throw new BaseException(StatusCode.NO_PRIVILEGE);
        }
    }

    @Override
    public List<UserClassRole> getCacheableUserClassRoleList(Integer userId, Integer classId) {
        //不允许userId和classId同时为空
        if (ObjectUtils.isEmpty(classId) || ObjectUtils.isEmpty(userId)) {
            log.warn("classId和userId不能为空");
            throw new BaseException(StatusCode.FAIL);
        }

        List<UserClassRole> userClassRoleList = getUserClassRoleListFromCache(userId, classId);
        //这里采用双检锁的方式获取数据除了保证一致性,也可以解决大量请求同时不命中直接请求数据库的问题
        if (ObjectUtils.isEmpty(userClassRoleList)) {
            //获取读锁(这里的读锁允许多个用户同时读取)
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(RedisPrefixConst.USER_CLASS_ROLE_LOCK_PREFIX + classId);
            RLock rLock = readWriteLock.readLock();
            //双重检查
            try {
                rLock.lock();

                userClassRoleList = getUserClassRoleListFromCache(userId, classId);
                if (ObjectUtils.isEmpty(userClassRoleList)) {
                    userClassRoleList = getUserClassRoleListFromDB(userId, classId);
                    if (ObjectUtils.isEmpty(userClassRoleList)) {
                        return null;
                    }
                    setUserClassRoleListToCache(userId, classId, userClassRoleList);
                }
            } finally {
                rLock.unlock();
            }
        }
        return userClassRoleList;
    }

    @Override
    @Transactional
    public void deleteUserClassRole(Integer userId, Integer classId) {
        //获取写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(RedisPrefixConst.USER_CLASS_ROLE_LOCK_PREFIX + classId);
        RLock wLock = readWriteLock.writeLock();

        try {
            wLock.lock();

            //userId为null则直接删除班级内的所有角色
            if (ObjectUtils.isEmpty(userId)) {
                //删除整个班级的角色缓存
                invalidateUserClassRoleCache(null, classId);
                //删除角色
                this.update(new LambdaUpdateWrapper<UserClassRole>()
                        .set(UserClassRole::getIsValid, 0)
                        .eq(UserClassRole::getClassId, classId));
            } else {
                /*userId和ClassId都不为null则删除班级内用户的角色*/
                //删除缓存
                invalidateUserClassRoleCache(userId, classId);
                //更新数据库,先删除用户在班级中的所有角色
                this.update(new LambdaUpdateWrapper<UserClassRole>()
                        .set(UserClassRole::getIsValid, 0)
                        .eq(UserClassRole::getUserId, userId)
                        .eq(UserClassRole::getClassId, classId));
            }
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void invalidateUserClassRoleCache(Integer userId, Integer classId) {
        if (ObjectUtils.isEmpty(userId)) {
            //userId不存在则删除整个班级的用户角色信息
            redisTemplate.delete(RedisPrefixConst.USER_CLASS_ROLE_PREFIX + classId);
        } else {
            //删除班级内特定用户的角色缓存信息
            redisTemplate.opsForHash().delete(RedisPrefixConst.USER_CLASS_ROLE_PREFIX + classId, userId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<UserClassRole> getUserClassRoleListFromCache(Integer userId, Integer classId) {
        return (List<UserClassRole>) redisTemplate.opsForHash().get(RedisPrefixConst.USER_CLASS_ROLE_PREFIX + classId, String.valueOf(userId));
    }

    private void setUserClassRoleListToCache(Integer userId, Integer classId, List<UserClassRole> userClassRoleList) {
        String key = RedisPrefixConst.USER_CLASS_ROLE_PREFIX + classId;
        redisTemplate.opsForHash().put(key, String.valueOf(userId), userClassRoleList);
        //设置缓存过期时间(这里如果报错直接记录日志,不影响业务)
        redisTemplate.expire(key, ClazzConst.USER_CLASS_ROLE_CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }

    private List<UserClassRole> getUserClassRoleListFromDB(Integer userId, Integer classId) {
        return this.list(new LambdaQueryWrapper<UserClassRole>()
                .eq(userId != null, UserClassRole::getUserId, userId)
                .eq(classId != null, UserClassRole::getClassId, classId)
                .eq(UserClassRole::getIsValid, 1));
    }
}




