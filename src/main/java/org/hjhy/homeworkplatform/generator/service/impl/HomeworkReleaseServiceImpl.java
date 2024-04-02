package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.MessageConstant;
import org.hjhy.homeworkplatform.constant.RoleConstant;
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
import org.springframework.context.annotation.Lazy;
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

    //创建一个线程池
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public HomeworkReleaseServiceImpl(@Lazy ClazzService clazzService, UserClassRoleService userClassRoleService, UserService userService, MessageService messageService, PushSettingService pushSettingService, HomeworkReminderMessageService homeworkReminderMessageService) {
        this.clazzService = clazzService;
        this.userClassRoleService = userClassRoleService;
        this.userService = userService;
        this.messageService = messageService;
        this.pushSettingService = pushSettingService;
        this.homeworkReminderMessageService = homeworkReminderMessageService;
    }

    @Override
    @Transactional
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
        this.save(homeworkRelease);

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
        var clazz = clazzService.getCachableClazz(homeworkRelease.getClassId());
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
    public void deleteHomework(Integer homeworkId) {
        this.update(new LambdaUpdateWrapper<HomeworkRelease>()
                .set(HomeworkRelease::getIsValid, 0)
                .eq(HomeworkRelease::getHomeworkId, homeworkId));
    }

    @Override
    public void updateHomework(Integer homeworkId, HomeworkReleaseDto homeworkReleaseDto) {
        var homeworkRelease = HomeworkRelease.builder()
                .homeworkId(homeworkId)
                .homeworkName(homeworkReleaseDto.getHomeworkName())
                .endTime(homeworkReleaseDto.getEndTime())
                .description(homeworkReleaseDto.getDescription()).build();
        this.updateById(homeworkRelease);
    }

    @Override
    public HomeworkReleaseVo findHomework(Integer homeworkId) {
        var homeworkRelease = this.getById(homeworkId);
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

        //todo 这里应该可以改的简洁一些
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
            var clazz = clazzService.getCachableClazz(homeworkRelease.getClassId());
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
}




