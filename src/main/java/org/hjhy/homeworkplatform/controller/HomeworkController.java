package org.hjhy.homeworkplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.annotation.HasRole;
import org.hjhy.homeworkplatform.annotation.RateLimiter;
import org.hjhy.homeworkplatform.annotation.RateRule;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseConditionDto;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseDto;
import org.hjhy.homeworkplatform.enums.LimitTypeEnum;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.HomeworkSubmission;
import org.hjhy.homeworkplatform.generator.service.HomeworkReleaseService;
import org.hjhy.homeworkplatform.generator.service.HomeworkSubmissionService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.*;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author HJHY
 * @date 2024/2/19 20:49
 */
@Tag(name = "作业模块接口")
@RestController
@Slf4j
public class HomeworkController {
    private final HomeworkReleaseService homeworkReleaseService;
    private final HomeworkSubmissionService homeworkSubmissionService;

    public HomeworkController(HomeworkReleaseService homeworkReleaseService, HomeworkSubmissionService homeworkSubmissionService) {
        this.homeworkReleaseService = homeworkReleaseService;
        this.homeworkSubmissionService = homeworkSubmissionService;
    }

    //接口幂等性可以由数据库唯一索引约束来保证
    @Operation(summary = "发布作业", description = "发布作业接口描述")
    @PostMapping("/homeworks")
    public Result<HomeworkRelease> saveHomework(@Valid @RequestBody HomeworkReleaseDto homeworkReleaseDto) {
        //这里切面没有办法进行权限检查,在业务中来进行权限检查
        if (ObjectUtils.isEmpty(homeworkReleaseDto.getClassId())) {
            log.warn("班级id为空不能通过");
            throw new BaseException("班级id不能为空");
        }

        HomeworkRelease homeworkRelease = homeworkReleaseService.saveHomework(RequestContext.getAuthInfo().getUserId(), homeworkReleaseDto);
        return Result.ok(homeworkRelease);
    }

    @Operation(summary = "删除作业", description = "删除作业接口描述")
    @HasRole(roles = RoleConstant.CLASS_CREATOR)
    @DeleteMapping("/homeworks/{homeworkId}")
    public Result<?> deleteHomework(@PathVariable Integer homeworkId) {
        homeworkReleaseService.deleteHomework(homeworkId);
        return Result.ok();
    }

    @Operation(summary = "更新作业", description = "更新作业接口描述")
    @HasRole(roles = RoleConstant.CLASS_CREATOR)
    @PutMapping("/homeworks/{homeworkId}")
    public Result<?> updateHomework(@PathVariable Integer homeworkId,
                                    @Valid @RequestBody HomeworkReleaseDto homeworkReleaseDto) {
        homeworkReleaseService.updateHomework(homeworkId, homeworkReleaseDto);
        return Result.ok();
    }

    @Operation(summary = "查找作业", description = "查找作业接口描述")
    @HasRole
    @GetMapping("/homeworks/{homeworkId}")
    public Result<HomeworkReleaseVo> findHomework(@PathVariable Integer homeworkId) {
        var homework = homeworkReleaseService.findHomework(homeworkId);
        return Result.ok(homework);
    }

    //条件查询的权限检查由业务完成(因此没办法获取到classId,无法进行权限检查)
    @Operation(summary = "条件查询作业", description = "条件查询作业接口描述")
    @GetMapping("/homeworks/condition")
    public Result<PageResult<HomeworkRelease>> condition(HomeworkReleaseConditionDto homeworkReleaseConditionDto,
                                                         @RequestParam(required = false) Integer current,
                                                         @RequestParam(required = false) Integer pageSize) throws InterruptedException {
        Page<HomeworkRelease> page = CommonUtils.getPage(current, pageSize);
        PageResult<HomeworkRelease> homeworkReleasePageResult = homeworkReleaseService.condition(homeworkReleaseConditionDto, page);
        return Result.ok(homeworkReleasePageResult);
    }

    @Operation(summary = "查找已提交作业列表", description = "查找已提交作业列表接口描述")
    @GetMapping("/homeworks/committed")
    public Result<PageResult<HomeworkSubmissionVo>> submitted(@RequestParam(required = false) Integer current,
                                                              @RequestParam(required = false) Integer pageSize) {
        Page<HomeworkSubmission> page = CommonUtils.getPage(current, pageSize);
        var userId = RequestContext.getAuthInfo().getUserId();
        var userHomework = homeworkSubmissionService.getSubmittedHomework(userId, page);
        return Result.ok(userHomework);
    }

    @Operation(summary = "查找未提交作业列表", description = "查找未提交作业列表接口描述")
    @GetMapping("/homeworks/uncommitted")
    public Result<PageResult<HomeworkRelease>> uncommitted(@RequestParam(required = false) Integer current,
                                                           @RequestParam(required = false) Integer pageSize) {
        Page<HomeworkRelease> page = CommonUtils.getPage(current, pageSize);
        var userId = RequestContext.getAuthInfo().getUserId();
        PageResult<HomeworkRelease> userHomework = homeworkReleaseService.getUncommittedHomework(userId, page);
        return Result.ok(userHomework);
    }

    @Operation(summary = "查找我发布的作业", description = "查找我发布的作业接口描述")
    @GetMapping("/homeworks/myLaunch")
    public Result<PageResult<HomeworkRelease>> myLaunch(@RequestParam(required = false) Integer current,
                                                        @RequestParam(required = false) Integer pageSize) {
        Page<HomeworkRelease> page = CommonUtils.getPage(current, pageSize);
        var userId = RequestContext.getAuthInfo().getUserId();
        var userHomework = homeworkReleaseService.myLaunch(userId, page);
        return Result.ok(userHomework);
    }

    /*********************************下面是作业提交的部分****************************************/
    //使用token接口的方式进行幂等性处理
    @Operation(summary = "提交作业", description = "提交作业接口描述")
    @HasRole(roles = {RoleConstant.CLASS_MEMBER})
    @PostMapping("/homeworks/{homeworkId}/submit")
    public Result<FileUploadVo> submit(@PathVariable Integer homeworkId,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false, defaultValue = ".docx") String fileSuffix,
                                       @RequestParam(required = false) String idempotentToken) throws Exception {
        var fileUploadVo = homeworkSubmissionService.submit(RequestContext.getAuthInfo().getUserId(), homeworkId, description, fileSuffix, idempotentToken);
        return Result.ok(fileUploadVo);
    }

    @Operation(summary = "阿里云对象存储作业提交回调", description = "阿里云对象存储作业提交回调接口描述")
    @PostMapping("/homeworks/callback/aliyun")
    public void homeworkSubmissionCallback(@RequestBody FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request, HttpServletResponse response) throws Exception {
        homeworkSubmissionService.callback(callbackBody, request, response);
    }

    //由于业务中需要进行并发处理，所以词用分布式锁来解决并发和接口幂等性问题
    @Operation(summary = "打包下载作业", description = "打包下载作业接口描述")
    @HasRole(roles = {RoleConstant.CLASS_CREATOR})
    @GetMapping("/homeworks/{homeworkId}/pack")
    public Result<?> pack(@PathVariable Integer homeworkId, @RequestParam String packFileName) throws Exception {
        var fileDownloadVo = homeworkSubmissionService.pack(homeworkId, packFileName);
        return Result.ok(fileDownloadVo);
    }

    @Operation(summary = "查找提交的作业记录", description = "查找提交的作业记录接口描述")
    @HasRole
    @GetMapping("/homeworks/{homeworkId}/history")
    public Result<List<HomeworkHistory>> history(@PathVariable Integer homeworkId) {
        var history = homeworkSubmissionService.history(RequestContext.getAuthInfo().getUserId(), homeworkId);
        return Result.ok(history);
    }

    //通过限流来做幂等性处理,30分钟允许通知一次
    @Operation(summary = "通知班级成员提交作业", description = "通知班级成员提交作业接口描述")
    @RateLimiter(limitType = LimitTypeEnum.HOMEWORK_ID, preventDuplicate = true, preventDuplicateRule = @RateRule(count = 1, time = 30 * 60))
    @HasRole(roles = {RoleConstant.CLASS_CREATOR, RoleConstant.CLASS_ADMIN})
    @GetMapping("/homeworks/{homeworkId}/notify")
    public Result<?> notifyHomework(@PathVariable Integer homeworkId) {
        homeworkSubmissionService.homeworkReminder(homeworkId);
        return Result.ok();
    }

}