package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.ClassDto;
import org.hjhy.homeworkplatform.dto.ClazzConditionDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.*;
import org.hjhy.homeworkplatform.generator.mapper.ClazzMapper;
import org.hjhy.homeworkplatform.generator.service.*;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.ClassInfoVo;
import org.hjhy.homeworkplatform.vo.PageResult;
import org.hjhy.homeworkplatform.vo.UserVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

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

    //创建一个线程池
    private final ThreadPoolExecutor executorForUser = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    //创建一个线程池
    private final ThreadPoolExecutor executorForClazz = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


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
        try {
            this.save(clazz);
        } catch (DuplicateKeyException e) {
            log.error("用户{{}}重复提交创建班级请求{}", userId, classDto);
            throw new BaseException(StatusCode.REPEAT_SUBMIT);
        }

        //维护班级权限表
        var userClassRole = UserClassRole.builder().classId(clazz.getClassId()).userId(userId).roleId(RoleConstant.CLASS_CREATOR.getRoleId()).build();
        try {
            userClassRoleService.save(userClassRole);
        } catch (DuplicateKeyException e) {
            log.error("用户{{}}在创建班级接口维护班级权限表时出现重复", userId);
            throw new BaseException(StatusCode.REPEAT_SUBMIT);
        }
    }

    @Transactional
    @Override
    public void updateClass(Integer classId, ClassDto classDto) {
        Clazz clazz = Clazz.builder().classId(classId).className(classDto.getClassName()).description(classDto.getDescription()).build();
        this.updateById(clazz);
    }

    @Override
    @Transactional
    public void deleteClass(Integer classId) {
        Clazz clazz = Clazz.builder().classId(classId).isValid(0).build();

        //检查班级内是否存在未结束作业
        LambdaQueryWrapper<HomeworkRelease> queryWrapper = new LambdaQueryWrapper<HomeworkRelease>()
                .eq(HomeworkRelease::getClassId, classId)
                .eq(HomeworkRelease::getIsValid, 1)
                .ge(HomeworkRelease::getEndTime, new Date());
        List<HomeworkRelease> existingHomeworkList = homeworkReleaseService.list(queryWrapper);
        if (!ObjectUtils.isEmpty(existingHomeworkList)) {
            throw new BaseException("班级内存在未结束的作业,请先结束作业再删除班级");
        }
        //不存在未结束的作业,则删除所有的作业
        LambdaUpdateWrapper<HomeworkRelease> homeworkUpdateQuery = new LambdaUpdateWrapper<HomeworkRelease>().set(HomeworkRelease::getIsValid, 0).eq(HomeworkRelease::getClassId, classId).eq(HomeworkRelease::getIsValid, 1);
        homeworkReleaseService.update(homeworkUpdateQuery);

        //不存在未结束的作业,删除班级内权限记录
        LambdaUpdateWrapper<UserClassRole> userClassRoleUpdateQuery = new LambdaUpdateWrapper<UserClassRole>().set(UserClassRole::getIsValid, 0).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getIsValid, 1);
        userClassRoleService.update(userClassRoleUpdateQuery);

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
        return clazzList.stream().map(ClassInfoVo::new).peek(classInfoVo -> classInfoVo.setCreatorName(userService.getById(classInfoVo.getCreatorId()).getRealname())).toList();
    }

    @Override
    public ClassInfoVo detail(Integer classId) {
        var clazz = this.getById(classId);
        //查询加入特定班级的用户详细信息
        var userIdList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getClassId, classId)).stream().map(UserClassRole::getUserId)
                //过滤掉创建者
                .filter(userId -> !userId.equals(clazz.getCreatorId())).toList();
        //查询相关用户详细信息
        var userVoList = userService.listByIds(userIdList).stream().map(UserVo::new).toList();
        //查询班级作业
        List<HomeworkRelease> homeworkList = homeworkReleaseService.list(new LambdaQueryWrapper<HomeworkRelease>().select(HomeworkRelease::getHomeworkId, HomeworkRelease::getHomeworkName, HomeworkRelease::getEndTime).eq(HomeworkRelease::getClassId, classId).eq(HomeworkRelease::getIsValid, 1));
        //查询用户在班级中的角色
        var roleIdList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().select(UserClassRole::getRoleId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getIsValid, 1)).stream().map(UserClassRole::getRoleId).toList();
        List<Role> roleList = roleService.listByIds(roleIdList);

        return new ClassInfoVo(clazz, userVoList, homeworkList, roleList);
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
        var existsed = userClassRoleService.exists(new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getUserId, userId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getIsValid, 1));
        if (existsed) {
            throw new BaseException(StatusCode.ALREADY_JOIN_CLASS);
        }

        //加入班级
        var userClassRole = UserClassRole.builder().userId(userId).classId(classId).roleId(RoleConstant.CLASS_MEMBER.getRoleId()).build();
        try {
            userClassRoleService.save(userClassRole);
        } catch (DuplicateKeyException e) {
            log.error("用户{{}}重复提交加入班级请求", userId);
            throw new BaseException(StatusCode.REPEAT_SUBMIT);
        }
    }

    @Override
    public void exitClass(Integer userId, Integer classId) {
        //检查是否是班级创建者
        Clazz clazz = this.getById(classId);
        if (clazz.getCreatorId().equals(RequestContext.getAuthInfo().getUserId())) {
            log.info("班级创建者无法退出班级");
            throw new BaseException("班级创建者无法退出班级");
        }

        var userClassRoleList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getUserId, userId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getIsValid, 1));

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
        return userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().select(UserClassRole::getUserId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId()).eq(UserClassRole::getIsValid, 1)).stream().map(UserClassRole::getUserId).toList();
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
        List<UserClassRole> userClassRoles = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().select(UserClassRole::getId, UserClassRole::getUserId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER.getRoleId()));
        for (Integer userId : userIdList) {
            if (userClassRoles.stream().noneMatch(userClassRole -> userClassRole.getUserId().equals(userId))) {
                log.info("用户{{}}不在班级内", userId);
                throw new BaseException("用户{%s}不在班级内".formatted(userId));
            }
        }

        //删除班级中的管理员再添加
        userClassRoleService.remove(new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getClassId, classId).eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId()));

        //添加
        var userClassRoleList = userIdList.stream().map(userId -> UserClassRole.builder().userId(userId).classId(classId).roleId(RoleConstant.CLASS_ADMIN.getRoleId()).build()).toList();
        userClassRoleService.saveBatch(userClassRoleList);
    }

    @Override
    public PageResult<ClassInfoVo> getJoinedClasses(Integer userId, Page<UserClassRole> page) {
        /*查询自己已经加入的班级id列表*/
        Page<UserClassRole> pageResult = userClassRoleService.page(page, new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getUserId, userId).eq(UserClassRole::getIsValid, 1).eq(UserClassRole::getRoleId, RoleConstant.CLASS_MEMBER.getRoleId()));
        long total = pageResult.getTotal();
        List<ClassInfoVo> list = pageResult.getRecords().stream().map(userClassRole -> ClassInfoVo.builder().creatorId(userClassRole.getUserId()).classId(userClassRole.getClassId()).build()).toList();

        //查询用户信息
        List<CompletableFuture<Void>> asyncUserTaskList = new ArrayList<>();
        for (ClassInfoVo classInfoVo : list) {
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                User user = userService.getById(classInfoVo.getCreatorId());
                classInfoVo.setCreatorName(user.getRealname());
            }, executorForUser);
            asyncUserTaskList.add(runAsync);
        }

        CompletableFuture.allOf(asyncUserTaskList.toArray(new CompletableFuture[0])).exceptionally(throwable -> {
            log.error("在查询加入的班级任务中,线程池中的获取用户信息的操作执行异常", throwable);
            throw new BaseException("查询用户信息任务执行异常");
        }).join();

        //查询班级信息
        List<CompletableFuture<Void>> asyncClazzTaskList = new ArrayList<>();
        for (ClassInfoVo classInfoVo : list) {
            CompletableFuture<Void> runAsync = CompletableFuture.runAsync(() -> {
                Clazz clazz = this.getById(classInfoVo.getClassId());
                classInfoVo.setClassName(clazz.getClassName());
            }, executorForClazz);
            asyncClazzTaskList.add(runAsync);
        }
        CompletableFuture.allOf(asyncClazzTaskList.toArray(new CompletableFuture[0])).exceptionally(throwable -> {
            log.error("在查询加入的班级任务中,线程池中的获取班级信息的操作执行异常", throwable);
            throw new BaseException("查询班级信息任务执行异常");
        }).join();

        return new PageResult<>(list, list.size(), total);
    }

    @Override
    public PageResult<ClassInfoVo> getCreatedClasses(Integer userId, Page<Clazz> page) throws InterruptedException {
        /*查询自己创建的班级id列表*/
        Page<Clazz> pageResult = this.page(page, new LambdaQueryWrapper<Clazz>().eq(Clazz::getCreatorId, userId).eq(Clazz::getIsValid, 1));
        long total = pageResult.getTotal();
        List<ClassInfoVo> records = pageResult.getRecords().stream().map(ClassInfoVo::new).toList();

        //采用CountDownLatch来等待线程池中的任务执行完毕
        CountDownLatch countDownLatch = new CountDownLatch(records.size());
        for (ClassInfoVo clazzInfo : records) {
            executorForClazz.submit(() -> {
                User user = userService.getById(clazzInfo.getCreatorId());
                clazzInfo.setCreatorName(user.getRealname());
                countDownLatch.countDown();
            });
        }

        boolean await = countDownLatch.await(3, TimeUnit.SECONDS);
        if (!await) {
            log.error("在查询创建的班级任务中,线程池中的获取用户信息的操作执行超时");
            throw new BaseException("任务执行超时");
        }
        return new PageResult<>(records, records.size(), total);
    }

    @Override
    public PageResult<Clazz> condition(Page<Clazz> page, ClazzConditionDto clazzConditionDto) {
        if (ObjectUtils.isEmpty(clazzConditionDto)) {
            throw new BaseException("查询条件不能为空");
        }

        Page<Clazz> pageResult = this.page(page, new LambdaQueryWrapper<Clazz>()
                .eq(clazzConditionDto.getClassName() != null, Clazz::getClassName, clazzConditionDto.getClassName())
                .eq(clazzConditionDto.getCreatorId() != null, Clazz::getCreatorId, clazzConditionDto.getCreatorId())
                .eq(clazzConditionDto.getDescription() != null, Clazz::getDescription, clazzConditionDto.getDescription())
                .eq(clazzConditionDto.getIsValid() != null, Clazz::getIsValid, clazzConditionDto.getIsValid()));

        //进行权限检查,前置切面无法完成检查
        for (Clazz clazz : pageResult.getRecords()) {
            userClassRoleService.checkUserClassPrivilege(RequestContext.getAuthInfo().getUserId(), clazz.getClassId(), new RoleConstant[]{RoleConstant.CLASS_CREATOR, RoleConstant.CLASS_ADMIN, RoleConstant.CLASS_MEMBER}, null);
        }

        return new PageResult<>(pageResult.getRecords(), pageResult.getRecords().size(), pageResult.getTotal());
    }
}




