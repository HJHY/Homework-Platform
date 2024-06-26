package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.ClazzConst;
import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.ClassDto;
import org.hjhy.homeworkplatform.dto.ClazzConditionDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.generator.mapper.ClazzMapper;
import org.hjhy.homeworkplatform.generator.service.ClazzService;
import org.hjhy.homeworkplatform.generator.service.HomeworkReleaseService;
import org.hjhy.homeworkplatform.generator.service.UserClassRoleService;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.ClassInfoVo;
import org.hjhy.homeworkplatform.vo.PageResult;
import org.hjhy.homeworkplatform.vo.UserVo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
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
    private final HomeworkReleaseService homeworkReleaseService;
    private final RedissonClient redissonClient;

    //创建一个线程池
    private final ThreadPoolExecutor executorForUser = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    //创建一个线程池
    private final ThreadPoolExecutor executorForClazz = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    //创建一个线程池
    private final ThreadPoolExecutor executorForDelayDelete = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public ClazzServiceImpl(UserClassRoleService userClassRoleService, UserService userService, RedisTemplate<String, Object> redisTemplate, HomeworkReleaseService homeworkReleaseService, RedissonClient redissonClient) {
        this.userClassRoleService = userClassRoleService;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.homeworkReleaseService = homeworkReleaseService;
        this.redissonClient = redissonClient;
    }

    @Transactional()
    @Override
    public Clazz createClass(Integer userId, ClassDto classDto) {
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

        return clazz;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateClass(Integer classId, ClassDto classDto) {
        Clazz clazz = Clazz.builder().classId(classId).className(classDto.getClassName()).description(classDto.getDescription()).build();
        //先改数据库再删除缓存
        this.updateById(clazz);
        invalidateCache(classId);

        //延迟双删(间隔10s)
        executorForDelayDelete.submit(() -> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            invalidateCache(classId);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Integer classId, Boolean mandatory) {
        //不强制删除则检查班级内是否存在未结束作业
        if (Boolean.FALSE.equals(mandatory)) {
            LambdaQueryWrapper<HomeworkRelease> queryWrapper = new LambdaQueryWrapper<HomeworkRelease>().eq(HomeworkRelease::getClassId, classId).eq(HomeworkRelease::getIsValid, 1).ge(HomeworkRelease::getEndTime, new Date());
            List<HomeworkRelease> existingHomeworkList = homeworkReleaseService.list(queryWrapper);
            if (!ObjectUtils.isEmpty(existingHomeworkList)) {
                throw new BaseException("班级内存在未结束的作业,请先结束作业再删除班级");
            }
        }
        //不存在未结束的作业,删除班级内的所有用户角色并做好缓存同步
        userClassRoleService.deleteUserClassRole(null, classId);
        //删除班级并缓存同步
        deleteClazzSync(classId);
        //删除班级分享码
        deleteShareCodeInfo(classId);
    }

    @Override
    public Clazz findClass(Integer classId) {
        return getCacheableClazz(classId);
    }

    @Override
    public List<ClassInfoVo> simpleInfo(List<Integer> classIdList) {
        var clazzList = this.listByIds(classIdList);

        //这里为了方便前端,将一些需要前端自行查询的也做了
        return clazzList.stream().map(ClassInfoVo::new).peek(classInfoVo -> classInfoVo.setCreatorName(userService.getById(classInfoVo.getCreatorId()).getRealname())).toList();
    }

    @Override
    public ClassInfoVo detail(Integer classId) {
        var clazz = getCacheableClazz(classId);
        //查询加入特定班级的用户详细信息
        var userIdList = userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().eq(UserClassRole::getClassId, classId)).stream().map(UserClassRole::getUserId)
                //过滤掉创建者
                .filter(userId -> !userId.equals(clazz.getCreatorId())).toList();
        //查询相关用户详细信息
        var userVoList = userService.listByIds(userIdList).stream().map(UserVo::new).toList();
        //查询班级作业
        List<HomeworkRelease> homeworkList = homeworkReleaseService.list(new LambdaQueryWrapper<HomeworkRelease>().select(HomeworkRelease::getHomeworkId, HomeworkRelease::getHomeworkName, HomeworkRelease::getEndTime).eq(HomeworkRelease::getClassId, classId).eq(HomeworkRelease::getIsValid, 1));

        return new ClassInfoVo(clazz, userVoList, homeworkList);
    }

    @Override
    public String shareClass(Integer userId, Integer classId) {
        return generateAndSetShareCode(classId);
    }

    @Transactional
    @Override
    public void joinClass(Integer userId, String shareCode) {
        var classId = getClassIdByShareCode(shareCode);
        if (ObjectUtils.isEmpty(classId)) {
            throw new BaseException(StatusCode.INVALID_CLASS_SHARE_CODE);
        }

        //检查是否已经加入班级
        List<UserClassRole> userClassRoleList = userClassRoleService.getCacheableUserClassRoleList(userId, classId);
        if (!ObjectUtils.isEmpty(userClassRoleList)) {
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
        Clazz clazz = getCacheableClazz(classId);
        if (clazz.getCreatorId().equals(RequestContext.getAuthInfo().getUserId())) {
            log.info("班级创建者无法退出班级");
            throw new BaseException("班级创建者无法退出班级");
        }

        var userClassRoleList = userClassRoleService.getCacheableUserClassRoleList(userId, classId);

        //已经退出班级
        if (ObjectUtils.isEmpty(userClassRoleList)) {
            throw new BaseException(StatusCode.ALREADY_EXIT_CLASS);
        }

        //退出班级,删除用户角色同时同步缓存
        userClassRoleService.deleteUserClassRole(userId, classId);
    }

    private String generateAndSetShareCode(Integer classId) {
        //检查班级里面是否存在share code
        var shareCode = getShareCode(classId);
        //采用双重检查锁来保证接口幂等(参考单例模式双检锁)
        if (ObjectUtils.isEmpty(shareCode)) {
            //加锁
            RLock lock = redissonClient.getLock(RedisPrefixConst.CLASS_SHARED_CODE_LOCK_PREFIX + classId);
            try {
                lock.lock();
                //再次检查
                shareCode = getShareCode(classId);
                if (ObjectUtils.isEmpty(shareCode)) {
                    //生成shared code
                    shareCode = CommonUtils.generateCode(SHARE_CODE_BIT);
                    //设置映射关系
                    setShareCode(classId, shareCode);
                }
            } finally {
                lock.unlock();
            }
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
        return (Integer) redisTemplate.opsForValue().get(RedisPrefixConst.SHARED_CODE_TO_CLASS_ID_PREFIX + shareCode);
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

    /**
     * 删除班级分享码的信息
     * @param classId classId
     */
    public void deleteShareCodeInfo(Integer classId) {
        //删除classId到shareCode的映射
        redisTemplate.delete(RedisPrefixConst.CLASS_ID_TO_SHARE_CODE_PREFIX + classId);
        //删除shareCode到classId的映射
        String shareCode = getShareCode(classId);
        redisTemplate.delete(RedisPrefixConst.SHARED_CODE_TO_CLASS_ID_PREFIX + shareCode);
    }

    @Override
    public List<Integer> queryClassAdmin(Integer classId) {
        return userClassRoleService.list(new LambdaQueryWrapper<UserClassRole>().select(UserClassRole::getUserId).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId()).eq(UserClassRole::getIsValid, 1)).stream().map(UserClassRole::getUserId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
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

        /*删除班级中的管理员再添加*/
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(RedisPrefixConst.USER_CLASS_ROLE_LOCK_PREFIX + classId);
        RLock wLock = readWriteLock.writeLock();
        try {
            wLock.lock();
            //删除缓存
            userClassRoleService.invalidateUserClassRoleCache(null, classId);
            //删除管理员
            LambdaUpdateWrapper<UserClassRole> updateWrapper = new LambdaUpdateWrapper<UserClassRole>().set(UserClassRole::getIsValid, 0).eq(UserClassRole::getClassId, classId).eq(UserClassRole::getRoleId, RoleConstant.CLASS_ADMIN.getRoleId()).eq(UserClassRole::getIsValid, 1);
            userClassRoleService.update(updateWrapper);

            //添加班级内用户角色信息
            var userClassRoleList = userIdList.stream().map(userId -> UserClassRole.builder().userId(userId).classId(classId).roleId(RoleConstant.CLASS_ADMIN.getRoleId()).build()).toList();
            userClassRoleService.saveBatch(userClassRoleList);
        } finally {
            wLock.unlock();
        }
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
                Clazz clazz = this.getCacheableClazz(classInfoVo.getClassId());
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

        LambdaQueryWrapper<Clazz> queryWrapper = new LambdaQueryWrapper<Clazz>()
                .eq(clazzConditionDto.getClassName() != null, Clazz::getClassName, clazzConditionDto.getClassName())
                .eq(clazzConditionDto.getCreatorId() != null, Clazz::getCreatorId, clazzConditionDto.getCreatorId())
                .eq(clazzConditionDto.getDescription() != null, Clazz::getDescription, clazzConditionDto.getDescription())
                .eq(clazzConditionDto.getIsValid() != null, Clazz::getIsValid, clazzConditionDto.getIsValid());
        Page<Clazz> pageResult = this.page(page, queryWrapper);

        //进行权限检查,前置切面无法完成检查
        for (Clazz clazz : pageResult.getRecords()) {
            userClassRoleService.checkUserClassPrivilege(RequestContext.getAuthInfo().getUserId(), clazz.getClassId(), new RoleConstant[]{RoleConstant.CLASS_CREATOR, RoleConstant.CLASS_ADMIN, RoleConstant.CLASS_MEMBER}, null);
        }

        return new PageResult<>(pageResult.getRecords(), pageResult.getRecords().size(), pageResult.getTotal());
    }

    @Override
    public void setClazzCache(Clazz clazz) {
        redisTemplate.opsForValue().set(RedisPrefixConst.CLASS_INFO_PREFIX + clazz.getClassId(), clazz, ClazzConst.CLASS_INFO_CACHE_EXPIRE_TIME, TimeUnit.HOURS);
    }

    @Override
    public Clazz getClazzFormCache(Integer classId) {
        return (Clazz) redisTemplate.opsForValue().get(RedisPrefixConst.CLASS_INFO_PREFIX + classId);
    }

    @Override
    public void invalidateCache(Integer classId) {
        redisTemplate.delete(RedisPrefixConst.CLASS_INFO_PREFIX + classId);
    }

    @Override
    public Clazz getClazzFromDB(Integer classId) {
        //这里不能直接通过id进行检查,还需要判断班级是否已经删除
        Clazz clazz = this.getById(classId);
        if (ObjectUtils.isEmpty(clazz) || clazz.getIsValid() == 0) {
            return null;
        }
        return clazz;
    }

    @Override
    public Clazz getCacheableClazz(Integer classId) {
        Clazz clazz = getClazzFormCache(classId);
        //使用双检锁
        if (ObjectUtils.isEmpty(clazz)) {
            RLock rLock = redissonClient.getReadWriteLock(RedisPrefixConst.CLASS_INFO_LOCK_PREFIX + classId).readLock();
            try {
                rLock.lock();
                //再检查一次缓存
                clazz = getClazzFormCache(classId);
                if (ObjectUtils.isEmpty(clazz)) {
                    clazz = getClazzFromDB(classId);
                    if (ObjectUtils.isEmpty(clazz)) {
                        return null;
                    }
                }
                //设置缓存
                setClazzCache(clazz);
            } finally {
                rLock.unlock();
            }
        }
        return clazz;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClazzSync(Integer classId) {
        //删除班级内的作业
        List<HomeworkRelease> homeworkReleaseList = homeworkReleaseService.list(new LambdaQueryWrapper<HomeworkRelease>()
                .eq(HomeworkRelease::getClassId, classId)
                .eq(HomeworkRelease::getIsValid, 1));
        //获取每一个作业的锁
        ArrayList<RLock> homeworkWLocks = new ArrayList<>(homeworkReleaseList.size());
        homeworkReleaseList.forEach(homeworkRelease -> homeworkWLocks.add(redissonClient.getReadWriteLock(RedisPrefixConst.HOMEWORK_INFO_LOCK_PREFIX + homeworkRelease.getHomeworkId()).writeLock()));
        //获取多个作业的组合锁
        RLock homeworkMultiLock = redissonClient.getMultiLock(homeworkWLocks.toArray(new RLock[0]));

        //获取班级写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(RedisPrefixConst.CLASS_INFO_LOCK_PREFIX + classId);
        RLock clazzLock = readWriteLock.writeLock();
        try {
            /*删除作业信息*/
            homeworkMultiLock.lock();
            for (HomeworkRelease homeworkRelease : homeworkReleaseList) {
                //删除缓存信息
                homeworkReleaseService.invalidateHomeworkCache(homeworkRelease.getHomeworkId());
                //判断作业是否已经删除
                HomeworkRelease homework = homeworkReleaseService.getHomeworkFromDb(homeworkRelease.getHomeworkId());
                if (ObjectUtils.isEmpty(homework) || homework.getIsValid() == 0) {
                    log.info("作业{{}}已经被删除", homeworkRelease.getHomeworkId());
                    continue;
                }
                homeworkRelease.setIsValid(0);
            }
            //删除作业
            homeworkReleaseService.updateBatchById(homeworkReleaseList);

            /*删除班级信息*/
            clazzLock.lock();
            log.info("删除操作加锁成功,锁内容{{}}", clazzLock.getName());
            //删除班级缓存
            invalidateCache(classId);
            //判断是否已经删除
            Clazz clazz = getClazzFromDB(classId);
            if (ObjectUtils.isEmpty(clazz) || clazz.getIsValid() == 0) {
                log.info("班级{{}}已经被删除", classId);
                return;
            }
            //删除班级
            clazz.setIsValid(0);
            this.updateById(clazz);

        } finally {
            //释放班级写锁
            clazzLock.unlock();
            //释放作业组合锁
            homeworkMultiLock.unlock();
            log.info("删除操作解锁成功,锁内容{{}}", clazzLock.getName());
        }
    }
}


