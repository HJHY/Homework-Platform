package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.MessageConstant;
import org.hjhy.homeworkplatform.context.ObjectStorageContext;
import org.hjhy.homeworkplatform.dto.EmailDto;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.dto.HomeworkStatusDto;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.HomeworkSubmission;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.mapper.HomeworkSubmissionMapper;
import org.hjhy.homeworkplatform.generator.service.*;
import org.hjhy.homeworkplatform.vo.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 13746
 * @description 针对表【homework_submission(作业提交表)】的数据库操作Service实现
 * @createDate 2024-02-19 20:47:41
 */
@Service
@Slf4j
public class HomeworkSubmissionServiceImpl extends ServiceImpl<HomeworkSubmissionMapper, HomeworkSubmission> implements HomeworkSubmissionService {

    private static final String HOMEWORK_FILE_PACKAGE_DIR = "packageDir/";
    private final UserService userService;
    private final ClazzService clazzService;
    private final HomeworkReleaseService homeworkReleaseService;
    private final ObjectStorageContext objectStorageContext;
    private final MessageService messageService;
    private final UserClassRoleService userClassRoleService;

    //创建一个线程池
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public HomeworkSubmissionServiceImpl(UserService userService, ClazzService clazzService, @Lazy HomeworkReleaseService homeworkReleaseService, ObjectStorageContext objectStorageContext, MessageService messageService, UserClassRoleService userClassRoleService) {
        this.userService = userService;
        this.clazzService = clazzService;
        this.homeworkReleaseService = homeworkReleaseService;
        this.objectStorageContext = objectStorageContext;
        this.messageService = messageService;
        this.userClassRoleService = userClassRoleService;
    }

    @Override
    @Transactional
    public FileUploadVo submit(Integer userId, Integer homeworkId, String description, String fileSuffix) throws Exception {
        var user = userService.getById(userId);
        var clazz = clazzService.getById(1);
        var homeworkRelease = homeworkReleaseService.getById(1);
        //这里应该要求所有名字构成的参数都不应该为null
        var filePath = homeworkId + "/" + clazz.getClassName() + "-" + user.getRealname() + "-" + homeworkRelease.getHomeworkName() + fileSuffix;
        log.info("文件全路径：{}", filePath);

        //保存提交记录
        var homeworkSubmission = HomeworkSubmission.builder().userId(userId).belongingId(homeworkId).description(description).fileSrc(filePath).status(0).build();
        this.save(homeworkSubmission);

        //执行文件上传策略
        var fileUploadVo = objectStorageContext.executeUploadStrategy(filePath, homeworkSubmission.getHomeworkId());
        log.info("文件上传结果：{}", fileUploadVo);

        //使用回调+延时任务的方式去双重查询作业状态(即使回调没有成功,也会在延时任务中进行查询)
        messageService.checkHomeworkStatus(new HomeworkStatusDto(homeworkSubmission.getHomeworkId(), filePath));

        return fileUploadVo;
    }

    @Override
    public void callback(FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request, HttpServletResponse response) throws Exception {
        //验证请求
        boolean res = objectStorageContext.verifyAndResponseUploadCallback(callbackBody, request);
        if (res) {
            //通过回调则去更新数据库的作业状态
            HomeworkSubmission homeworkSubmission = HomeworkSubmission.builder().homeworkId(callbackBody.getHomeworkId()).status(1).build();
            this.updateById(homeworkSubmission);
            log.info("作业提交回调成功,作业id为{},并成功更新装也状态", callbackBody.getHomeworkId());
        }
        //响应回调
        objectStorageContext.responseUploadCallback(request, response, res);
    }

    @Override
    public FileDownloadVo pack(Integer homeworkId, String packFileName) throws Exception {
        //如果只是在统一输出的结果文件夹中,很有可能出现不同作业打包名相同的情况,还是加上作业id作为文件路径前缀
        var sourceDir = homeworkId + "/";
        var destFilePath = HOMEWORK_FILE_PACKAGE_DIR + homeworkId + "/" + packFileName;

        return objectStorageContext.executeDownloadStrategy(sourceDir, destFilePath);
    }

    @Override
    public List<HomeworkHistory> history(Integer userId, Integer homeworkId) {
        List<HomeworkSubmission> list = this.list(new LambdaQueryWrapper<HomeworkSubmission>()
                .eq(HomeworkSubmission::getBelongingId, homeworkId)
                .eq(HomeworkSubmission::getUserId, userId)
                .eq(HomeworkSubmission::getIsValid, 1));
        return list.stream().map(HomeworkHistory::new).toList();
    }

    @Override
    public void homeworkReminder(Integer homeworkId) {
        /*查询用户是否已经提交作业,已提交的不需要再次通知*/
        HomeworkRelease homeworkRelease = homeworkReleaseService.getById(homeworkId);

        List<Integer> userIdList = userClassRoleService.getStudentIdInClazz(homeworkRelease.getClassId());
        userIdList.forEach(userId -> executor.submit(() -> {
            //检查用户的作业提交状态
            boolean existsed = this.exists(
                    new LambdaQueryWrapper<HomeworkSubmission>()
                            .eq(HomeworkSubmission::getUserId, userId)
                            .eq(HomeworkSubmission::getBelongingId, homeworkId)
                            .eq(HomeworkSubmission::getStatus, 1)
            );
            //用户已经提交则不需要推送提醒信息
            if (existsed) {
                return;
            }

            //推送提醒信息
            User user = userService.getById(userId);
            Clazz clazz = clazzService.getById(homeworkRelease.getClassId());

            String notifyMessage = MessageConstant.HOMEWORK_REMINDER_MESSAGE.formatted(
                    user.getRealname(),
                    clazz.getClassName(),
                    homeworkRelease.getHomeworkName(),
                    homeworkRelease.getEndTime(),
                    homeworkRelease.getDescription()
            );

            //往消息队列中发送消息
            var emailDto = EmailDto.builder().toEmail(user.getEmail()).subject("作业提醒通知").content(notifyMessage).build();
            messageService.sendEmail(emailDto);
        }));
    }

    @Override
    public PageResult<HomeworkSubmissionVo> getSubmittedHomework(Integer userId, Page<HomeworkSubmission> page) {
        IPage<HomeworkSubmissionVo> committedHomework = this.baseMapper.getSubmittedHomework(userId, page);
        List<HomeworkSubmissionVo> records = committedHomework.getRecords();
        long total = committedHomework.getTotal();
        return new PageResult<>(records, total);
    }
}




