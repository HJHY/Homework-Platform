package org.hjhy.homeworkplatform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.PushSetting;
import org.hjhy.homeworkplatform.generator.service.PushSettingService;
import org.hjhy.homeworkplatform.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author HJHY
 * @date 2024/2/7 17:21
 */
@RestController
@RequestMapping("/setting")
@Tag(name = "设置模块接口")
public class SettingController {
    private final PushSettingService pushSettingService;

    public SettingController(PushSettingService pushSettingService) {
        this.pushSettingService = pushSettingService;
    }

    @Operation(summary = "设置DDL提醒时间", description = "设置DDL提醒时间")
    @PostMapping("/ddls")
    public Result<?> setDdlPush(@RequestParam(name = "ddlSettingList") List<Integer> ddlSettingList) throws BaseException {
        pushSettingService.setDdls(RequestContext.getAuthInfo().getUserId(), ddlSettingList);
        return Result.ok();
    }

    @Operation(summary = "查询DDL提醒时间", description = "查询DDL提醒时间")
    @GetMapping("/ddls")
    public Result<List<PushSetting>> listDdls() {
        var ddlPushList = pushSettingService.listDdls(RequestContext.getAuthInfo().getUserId());
        return Result.ok(ddlPushList, "时间以小时为单位");
    }
}
