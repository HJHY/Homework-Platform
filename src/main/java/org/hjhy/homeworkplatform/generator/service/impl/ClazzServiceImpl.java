package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.ClassDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.Role;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.generator.mapper.ClazzMapper;
import org.hjhy.homeworkplatform.generator.service.*;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.ClassInfoVo;
import org.hjhy.homeworkplatform.vo.RelationVo;
import org.hjhy.homeworkplatform.vo.UserVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author 13746
 * @description 针对表【clazz(班级表)】的数据库操作Service实现
 * @createDate 2024-02-08 17:08:47
 */
@Service
@Slf4j
public class ClazzServiceImpl extends ServiceImpl<ClazzMapper, Clazz> implements ClazzService {
    /**
     * 班级分享码的有效时间
     */
    public static final Duration SHARE_CODE_DURATION = Duration.ofDays(10);

    /**
     * 班级分享码的位数
     */
    private static final Integer SHARE_CODE_BIT = 32;

    private final UserClassRoleService userClassRoleService;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RoleService roleService;
    private final HomeworkReleaseService homeworkReleaseService;

    public ClazzServiceImpl(UserClassRoleService userClassRoleService, UserService userService, RedisTemplate<String, Object> redisTemplate, RoleService roleService, HomeworkReleaseService homeworkReleaseService) {
        this.userClassRoleService = userClassRoleService;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.roleService = roleService;
        this.homeworkReleaseService = homeworkReleaseService;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createClass(Integer userId, ClassDto classDto) {
        //检查是否存在同名班级
        if (this.exists(new LambdaQueryWrapper<Clazz>().eq(Clazz::getClassName, classDto.getClassName()))) {
            throw new BaseException(StatusCode.DUPLICATED_CLASS_NAME);
        }

        var clazz = Clazz.builder().creatorId(userId).className(classDto.getClassName()).description(classDto.getDescription()).build();
        this.save(clazz);

        //维护班级权限表
        var userClassRole = UserClassRole.builder().classId(clazz.getClassId()).userId(userId).roleId(RoleConstant.CLASS_CREATOR.getRoleId()).build();
        userClassRoleService.save(userClassRole);
    }

    @Transactional
    @Override
    public void updateClass(Integer classId, ClassDto classDto) {
        Clazz clazz = Clazz.builder().classId(classId).className(classDto.getClassName()).description(classDto.getDescription()).build();
        this.updateById(clazz);
    }

    @Override
    public void deleteClass(Integer classId) {
        Clazz clazz = Clazz.builder().classId(classId).isValid(0).build();
        this.updateById(clazz);
    }

    @Override
    public Clazz findClass(Integer classId) {
        return this.getById(classId);
    }

    @Override
    public List<ClassInfoVo> simpleInfo(List<Integer> classIdList) {
        var clazzList = this.listByIds(classIdList);

        //这里为了方便前端,将一些需要前端自行查询的也做了
        return clazzList.stream()
                .map(ClassInfoVo::new)
                .peek(classInfoVo -> classInfoVo.setCreatorName(userService.getById(classInfoVo.getCreatorId()).getRealname()))
                .toList();
    }

    @Override
    public ClassInfoVo detail(Integer classId) {
        var clazz = this.getById(classId);
        //查询加入特定班级的用户详细信息
        var userIdList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                        .eq(UserClassRole::getClassId, classId))
                .stream()
                .map(UserClassRole::getUserId)
                //过滤掉创建者
                .filter(userId -> !userId.equals(clazz.getCreatorId())).toList();
        //查询相关用户详细信息
        var userVoList = userService.listByIds(userIdList).stream().map(UserVo::new).toList();
        //查询班级作业
        List<HomeworkRelease> homeworkList = homeworkReleaseService.list(
                new LambdaQueryWrapper<HomeworkRelease>()
                        .select(HomeworkRelease::getHomeworkId, HomeworkRelease::getHomeworkName, HomeworkRelease::getEndTime)
                        .eq(HomeworkRelease::getClassId, classId)
                        .eq(HomeworkRelease::getIsValid, 1));
        //查询用户在班级中的角色
        var roleIdList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                        .select(UserClassRole::getRoleId)
                        .eq(UserClassRole::getClassId, classId)
                        .eq(UserClassRole::getIsValid, 1))
                .stream().map(UserClassRole::getRoleId).toList();
        List<Role> roleList = roleService.listByIds(roleIdList);

        return new ClassInfoVo(clazz, userVoList, homeworkList, roleList);
    }

    @Override
    public RelationVo relation(Integer userId) {
        /*查询自己创建的班级id列表*/
        CompletableFuture<List<Integer>> createdClassIdListTask = CompletableFuture.supplyAsync(() -> this.list(new LambdaQueryWrapper<Clazz>()
                        .select(Clazz::getClassId)
                        .eq(Clazz::getCreatorId, userId)
                        .eq(Clazz::getIsValid, 1))
                .stream().map(Clazz::getClassId).toList());

        /*查询自己已经加入的班级id列表*/
        CompletableFuture<List<Integer>> joinedClassIdListTask = CompletableFuture.supplyAsync(() -> userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                        .select(UserClassRole::getClassId)
                        .eq(UserClassRole::getUserId, userId)
                        .eq(UserClassRole::getIsValid, 1)
                        .eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER.getRoleId()))
                .stream().map(UserClassRole::getClassId).toList());

        CompletableFuture.allOf(createdClassIdListTask, joinedClassIdListTask).join();

        return new RelationVo(createdClassIdListTask.join(), joinedClassIdListTask.join());
    }

    @Override
    public String shareClass(Integer userId, Integer classId) {
        return generateAndSetShareCode(classId);
    }

    @Transactional
    @Override
    public void joinClass(Integer userId, String shareCode) {
        var classId = getClassIdByShareCode(shareCode);

        //检查是否已经加入班级
        var existsed = userClassRoleService.exists(new LambdaQueryWrapper<UserClassRole>()
                .eq(UserClassRole::getUserId, userId)
                .eq(UserClassRole::getClassId, classId)
                .eq(UserClassRole::getIsValid, 1));
        if (existsed) {
            throw new BaseException(StatusCode.ALREADY_JOIN_CLASS);
        }

        //加入班级
        var userClassRole = UserClassRole.builder()
                .userId(userId)
                .classId(classId)
                .roleId(RoleConstant.CLASS_MEMBER.getRoleId())
                .build();
        userClassRoleService.save(userClassRole);
    }

    @Override
    public void exitClass(Integer userId, Integer classId) {
        //检查是否是班级创建者
        Clazz clazz = this.getById(classId);
        if (clazz.getCreatorId().equals(RequestContext.getAuthInfo().getUserId())) {
            log.info("班级创建者无法退出班级");
            throw new BaseException("班级创建者无法退出班级");
        }

        var userClassRoleList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                .eq(UserClassRole::getUserId, userId)
                .eq(UserClassRole::getClassId, classId)
                .eq(UserClassRole::getIsValid, 1));

        //已经退出班级
        if (ObjectUtils.isEmpty(userClassRoleList)) {
            throw new BaseException(StatusCode.ALREADY_EXIT_CLASS);
        }

        //退出班级
        userClassRoleList.forEach(userClassRole -> userClassRole.setIsValid(0));
        userClassRoleService.updateBatchById(userClassRoleList);
    }

    private String generateAndSetShareCode(Integer classId) {
        //检查班级里面是否存在share code
        var shareCode = getShareCode(classId);
        if (ObjectUtils.isEmpty(shareCode)) {
            //生成shared code
            shareCode = CommonUtils.generateCode(SHARE_CODE_BIT);
            //设置映射关系
            setShareCode(classId, shareCode);
        }
        return shareCode;
    }

    /**
     * 将shareCode设置到缓存中
     *
     * @param classId   classId
     * @param shareCode shareCode
     */
    private void setShareCode(Integer classId, String shareCode) {
        //设置classId到shareCode的映射
        redisTemplate.opsForValue().set(RedisPrefixConst.CLASS_ID_TO_SHARE_CODE_PREFIX + classId, shareCode, SHARE_CODE_DURATION);
        //设置shareCode到classId的映射
        redisTemplate.opsForValue().set(RedisPrefixConst.SHARED_CODE_TO_CLASS_ID_PREFIX + shareCode, classId, SHARE_CODE_DURATION);
    }

    /**
     * 通过shareCode获取班级id
     *
     * @param shareCode shareCode
     * @return classId
     */
    private Integer getClassIdByShareCode(String shareCode) throws BaseException {
        var classId = (Integer) redisTemplate.opsForValue().get(RedisPrefixConst.SHARED_CODE_TO_CLASS_ID_PREFIX + shareCode);
        if (classId == null) {
            throw new BaseException(StatusCode.INVALID_CLASS_SHARE_CODE);
        }
        return classId;
    }

    /**
     * 检查班级里面是否存在share code
     *
     * @param classId classId
     * @return shared code
     */
    public String getShareCode(Integer classId) {
        return (String) redisTemplate.opsForValue().get(RedisPrefixConst.CLASS_ID_TO_SHARE_CODE_PREFIX + classId);
    }

    @Override
    public List<Integer> queryClassAdmin(Integer classId) {
        return userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                        .select(UserClassRole::getUserId)
                        .eq(UserClassRole::getClassId, classId)
                        .eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId())
                        .eq(UserClassRole::getIsValid, 1))
                .stream().map(UserClassRole::getUserId).toList();
    }

    @Transactional
    @Override
    public void setClassAdmin(Integer classId, List<Integer> userIdList) {
        if (ObjectUtils.isEmpty(userIdList)) {
            return;
        }

        if (userIdList.contains(RequestContext.getAuthInfo().getUserId())) {
            log.info("不能将自己设置为班级管理员");
            throw new BaseException("不能将自己设置为班级管理员");
        }

        //判断将要设置为管理员的用户是否都已经在班级内
        List<UserClassRole> userClassRoles = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>()
                .select(UserClassRole::getId, UserClassRole::getUserId)
                .eq(UserClassRole::getClassId, classId)
                .eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER.getRoleId()));
        for (Integer userId : userIdList) {
            if (userClassRoles.stream().noneMatch(userClassRole -> userClassRole.getUserId().equals(userId))) {
                log.info("用户{{}}不在班级内", userId);
                throw new BaseException("用户{%s}不在班级内".formatted(userId));
            }
        }

        //删除班级中的管理员再添加
        userClassRoleService.remove(new LambdaQueryWrapper<UserClassRole>()
                .eq(UserClassRole::getClassId, classId)
                .eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId()));

        //添加
        var userClassRoleList = userIdList.stream()
                .map(userId -> UserClassRole.builder().userId(userId).classId(classId).roleId(RoleConstant.CLASS_ADMIN.getRoleId()).build())
                .toList();
        userClassRoleService.saveBatch(userClassRoleList);
    }
}




