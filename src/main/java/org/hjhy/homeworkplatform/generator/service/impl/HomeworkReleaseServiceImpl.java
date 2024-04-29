package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.*;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseConditionDto;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseDto;
import org.hjhy.homeworkplatform.dto.HomeworkReminderDto;
import org.hjhy.homeworkplatform.enums.MessageRecordEnum;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.HomeworkReminderMessage;
import org.hjhy.homeworkplatform.generator.domain.PushSetting;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.mapper.HomeworkReleaseMapper;
import org.hjhy.homeworkplatform.generator.service.*;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.HomeworkReleaseVo;
import org.hjhy.homeworkplatform.vo.PageResult;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 13746
 * @description 针对表【homework_release(作业发布表)】的数据库操作Service实现
 * @createDate 2024-02-19 20:47:51
 */
@Service
@Slf4j
public class HomeworkReleaseServiceImpl extends ServiceImpl<HomeworkReleaseMapper, HomeworkRelease> implements HomeworkReleaseService {
    private final ClazzService clazzService;
    private final UserClassRoleService userClassRoleService;
    private final UserService userService;
    private final MessageService messageService;
    private final PushSettingService pushSettingService;
    private final HomeworkReminderMessageService homeworkReminderMessageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    //创建一个线程池
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    //创建一个线程池(用于缓存延迟双删)
    private final ThreadPoolExecutor executorForCacheDelayDelete = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public HomeworkReleaseServiceImpl(@Lazy ClazzService clazzService, UserClassRoleService userClassRoleService, UserService userService, MessageService messageService, PushSettingService pushSettingService, HomeworkReminderMessageService homeworkReminderMessageService, RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient) {
        this.clazzService = clazzService;
        this.userClassRoleService = userClassRoleService;
        this.userService = userService;
        this.messageService = messageService;
        this.pushSettingService = pushSettingService;
        this.homeworkReminderMessageService = homeworkReminderMessageService;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HomeworkRelease saveHomework(Integer userId, HomeworkReleaseDto homeworkReleaseDto) {
        //班级拦截器没有办法对班级信息做检查,因此这里需要手动检查班级的存在性
        // 获取HttpServletRequest对象
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        userClassRoleService.checkUserClassPrivilege(userId, homeworkReleaseDto.getClassId(), new RoleConstant[]{RoleConstant.CLASS_CREATOR}, request);

        //保存作业
        var homeworkRelease = HomeworkRelease.builder()
                .classId(homeworkReleaseDto.getClassId())
                .homeworkName(homeworkReleaseDto.getHomeworkName())
                .creatorId(userId)
                .description(homeworkReleaseDto.getDescription())
                .endTime(homeworkReleaseDto.getEndTime()).build();

        try {
            this.save(homeworkRelease);
        } catch (DuplicateKeyException e) {
            log.error("重复发布内容相同的作业{{}}", homeworkReleaseDto);
            throw new BaseException(StatusCode.REPEAT_SUBMIT);
        }

        //推送作业发布通知
        notifyHomework(homeworkRelease);

        return homeworkRelease;
    }

    /**
     * 进行作业发布通知和DDL提醒信息推送
     */
    private void notifyHomework(HomeworkRelease homeworkRelease) {
        LocalDateTime endDateTime = homeworkRelease.getEndTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        List<Integer> userIdList = userClassRoleService.getStudentIdInClazz(homeworkRelease.getClassId());
        userIdList.forEach(userId -> {
            //作业发布通知
            notifyHomeworkReleaseMessage(userId, homeworkRelease);

            //按照用户的DDL设置进行未来的提醒信息推送
            List<PushSetting> pushSettingList = pushSettingService.list(new LambdaQueryWrapper<PushSetting>()
                    .eq(PushSetting::getUserId, userId));

            ArrayList<HomeworkReminderMessage> messageRecordList = new ArrayList<>();
            for (PushSetting pushSetting : pushSettingList) {
                //计算推送时间
                LocalDateTime pushTime = endDateTime.minusHours(pushSetting.getAdvanceTime());
                //如果是当天则推送到延迟消息队列
                if (pushTime.getDayOfYear() == LocalDate.now().getDayOfYear()) {
                    //根据推送时间和当前时间计算延迟的时间
                    long delayTime = CommonUtils.getDelayTime(LocalDateTime.now(), pushTime);
                    //向队列推送DDL日期在当天的作业提醒
                    HomeworkReminderDto homeworkReminderDto = new HomeworkReminderDto(null, userId, homeworkRelease.getHomeworkId());
                    messageService.sendHomeworkDelayReminderEmail(homeworkReminderDto, delayTime);
                } else {
                    //记录到数据库中由定时任务扫描并去写道消息队列
                    Date dealTime = Date.from(pushTime.atZone(ZoneId.systemDefault()).toInstant());
                    HomeworkReminderMessage messageRecord = HomeworkReminderMessage.builder()
                            .userId(userId)
                            .homeworkId(homeworkRelease.getHomeworkId())
                            .method(MessageRecordEnum.EMAIL.getCode())
                            .dealTime(dealTime)
                            .status(0).build();
                    messageRecordList.add(messageRecord);
                }
            }
            //批量保存
            if (!ObjectUtils.isEmpty(messageRecordList)) {
                homeworkReminderMessageService.saveBatch(messageRecordList);
            }
        });
    }

    private void notifyHomeworkReleaseMessage(Integer userId, HomeworkRelease homeworkRelease) {
        var clazz = clazzService.getCacheableClazz(homeworkRelease.getClassId());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var endTime = simpleDateFormat.format(homeworkRelease.getEndTime());
        //采用异步的方式去执行可以极大提高效率
        executor.submit(() -> {
            User user = userService.getById(userId);

            //往消息队列中发送消息
            String notifyMessage = MessageConstant.HOMEWORK_RELEASE_MESSAGE.formatted(
                    user.getRealname(),
                    clazz.getClassName(),
                    homeworkRelease.getHomeworkName(),
                    endTime,
                    homeworkRelease.getDescription());
            var emailDto = EmailDto.builder().toEmail(user.getEmail()).subject("作业发布通知").content(notifyMessage).build();
            messageService.sendEmail(emailDto);
            log.info("将作业{{}}的提醒信息推送至用户{{}}发送到消息队列", homeworkRelease.getHomeworkId(), userId);
        });
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHomework(Integer homeworkId) {
        deleteHomeworkSync(homeworkId);
    }

    @Override
    public void updateHomework(Integer homeworkId, HomeworkReleaseDto homeworkReleaseDto) {
        //先改数据库再删除缓存
        var homeworkRelease = HomeworkRelease.builder()
                .homeworkId(homeworkId)
                .homeworkName(homeworkReleaseDto.getHomeworkName())
                .endTime(homeworkReleaseDto.getEndTime())
                .description(homeworkReleaseDto.getDescription()).build();
        this.updateById(homeworkRelease);
        //删除缓存
        invalidateHomeworkCache(homeworkId);
        //延迟双删
        executorForCacheDelayDelete.submit(() -> {
            try {
                //延迟5s再删除一次
                Thread.sleep(5000);
                invalidateHomeworkCache(homeworkId);
            } catch (InterruptedException e) {
                log.error("缓存延迟双删失败");
            }
        });
    }

    @Override
    public HomeworkReleaseVo findHomework(Integer homeworkId) {
        var homeworkRelease = getCacheableHomework(homeworkId);
        return new HomeworkReleaseVo(homeworkRelease);
    }

    @Override
    public PageResult<HomeworkRelease> myLaunch(Integer userId, Page<HomeworkRelease> page) {
        var queryWrapper = new LambdaQueryWrapper<HomeworkRelease>();
        queryWrapper.eq(HomeworkRelease::getCreatorId, userId)
                .eq(HomeworkRelease::getIsValid, 1);

        var pageResult = this.page(page, queryWrapper);
        log.info(String.valueOf(pageResult));
        return new PageResult<>(pageResult.getRecords(), pageResult.getRecords().size(), pageResult.getTotal());
    }

    @Override
    public PageResult<HomeworkRelease> getUncommittedHomework(Integer userId, Page<HomeworkRelease> page) {
        IPage<HomeworkRelease> unSubmittedHomework = this.getBaseMapper().getUnSubmittedHomework(userId, page, RoleConstant.CLASS_MEMBER.getRoleId());
        return new PageResult<>(unSubmittedHomework.getRecords(), unSubmittedHomework.getRecords().size(), unSubmittedHomework.getTotal());
    }

    @Override
    public PageResult<HomeworkRelease> condition(HomeworkReleaseConditionDto homeworkReleaseConditionDto, Page<HomeworkRelease> page) throws InterruptedException {
        if (ObjectUtils.isEmpty(homeworkReleaseConditionDto)) {
            throw new BaseException("查询条件不能为空");
        }

        LambdaQueryWrapper<HomeworkRelease> queryWrapper = new LambdaQueryWrapper<HomeworkRelease>()
                .eq(homeworkReleaseConditionDto.getHomeworkId() != null, HomeworkRelease::getHomeworkId, homeworkReleaseConditionDto.getHomeworkId())
                .eq(homeworkReleaseConditionDto.getClassId() != null, HomeworkRelease::getClassId, homeworkReleaseConditionDto.getClassId())
                .eq(homeworkReleaseConditionDto.getHomeworkName() != null, HomeworkRelease::getHomeworkName, homeworkReleaseConditionDto.getHomeworkName())
                .eq(homeworkReleaseConditionDto.getCreatorId() != null, HomeworkRelease::getCreatorId, homeworkReleaseConditionDto.getCreatorId())
                .eq(homeworkReleaseConditionDto.getIsValid() != null, HomeworkRelease::getIsValid, homeworkReleaseConditionDto.getIsValid())
                .eq(homeworkReleaseConditionDto.getDescription() != null, HomeworkRelease::getDescription, homeworkReleaseConditionDto.getDescription())
                .ge(homeworkReleaseConditionDto.getLaunchTime() != null, HomeworkRelease::getLaunchTime, homeworkReleaseConditionDto.getLaunchTime())
                .le(homeworkReleaseConditionDto.getEndTime() != null, HomeworkRelease::getEndTime, homeworkReleaseConditionDto.getEndTime());
        Page<HomeworkRelease> pageResult = this.page(page, queryWrapper);

        //检查权限
        CountDownLatch countDownLatch = new CountDownLatch(pageResult.getRecords().size());
        pageResult.getRecords().forEach(homeworkRelease -> executor.submit(() -> {
            var clazz = clazzService.getCacheableClazz(homeworkRelease.getClassId());
            userClassRoleService.checkUserClassPrivilege(homeworkRelease.getCreatorId(), clazz.getClassId(), new RoleConstant[]{RoleConstant.CLASS_CREATOR}, null);
            countDownLatch.countDown();
        }));

        boolean await = countDownLatch.await(3, TimeUnit.SECONDS);
        if (!await) {
            log.error("作业条件查询时权限检查超时");
            throw new BaseException("作业条件查询时权限检查超时");
        }

        return new PageResult<>(pageResult.getRecords(), pageResult.getRecords().size(), pageResult.getTotal());
    }

    @Override
    public void setHomeworkCache(HomeworkRelease homeworkRelease) {
        redisTemplate.opsForValue().set(RedisPrefixConst.HOMEWORK_INFO_PREFIX + homeworkRelease.getHomeworkId(), homeworkRelease, HomeworkConst.HOMEWORK_INFO_CACHE_TIME, TimeUnit.HOURS);
    }

    @Override
    public HomeworkRelease getHomeworkFromCache(Integer homeworkId) {
        return (HomeworkRelease) redisTemplate.opsForValue().get(RedisPrefixConst.HOMEWORK_INFO_PREFIX + homeworkId);
    }

    @Override
    public void invalidateHomeworkCache(Integer homeworkId) {
        redisTemplate.delete(RedisPrefixConst.HOMEWORK_INFO_PREFIX + homeworkId);
    }

    @Override
    public HomeworkRelease getHomeworkFromDb(Integer homeworkId) {
        HomeworkRelease homeworkRelease = this.getById(homeworkId);
        if (ObjectUtils.isEmpty(homeworkRelease) || homeworkRelease.getIsValid() == 0) {
            return null;
        }
        return homeworkRelease;
    }

    @Override
    public HomeworkRelease getCacheableHomework(Integer homeworkId) {
        HomeworkRelease homeworkRelease = getHomeworkFromCache(homeworkId);
        if (ObjectUtils.isEmpty(homeworkRelease)) {
            RLock rLock = redissonClient.getReadWriteLock(RedisPrefixConst.HOMEWORK_INFO_LOCK_PREFIX + homeworkId).readLock();
            //加锁
            try {
                rLock.lock();
                homeworkRelease = getHomeworkFromCache(homeworkId);
                if (ObjectUtils.isEmpty(homeworkRelease)) {
                    homeworkRelease = getHomeworkFromDb(homeworkId);
                    if (ObjectUtils.isEmpty(homeworkRelease)) {
                        log.warn("作业{{}}不存在或已被删除", homeworkId);
                        return null;
                    }
                    setHomeworkCache(homeworkRelease);
                }
            } finally {
                rLock.unlock();
            }
        }
        return homeworkRelease;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHomeworkSync(Integer homeworkId) {
        //获取写锁
        RLock wLock = redissonClient.getReadWriteLock(RedisPrefixConst.HOMEWORK_INFO_LOCK_PREFIX + homeworkId).writeLock();
        try {
            wLock.lock();
            log.info("删除作业{{}}加锁成功", homeworkId);
            invalidateHomeworkCache(homeworkId);
            //判断是否已经删除
            HomeworkRelease homeworkRelease = this.getById(homeworkId);
            if (ObjectUtils.isEmpty(homeworkRelease) || homeworkRelease.getIsValid() == 0) {
                log.info("作业{{}}已经被删除,无需再次删除", homeworkId);
                return;
            }
            //删除作业
            homeworkRelease.setIsValid(0);
            this.updateById(homeworkRelease);
        } finally {
            wLock.unlock();
            log.info("删除作业{{}}解锁成功", homeworkId);
        }
    }
}




