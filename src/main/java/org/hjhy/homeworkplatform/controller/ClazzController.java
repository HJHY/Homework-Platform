package org.hjhy.homeworkplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.hjhy.homeworkplatform.annotation.HasRole;
import org.hjhy.homeworkplatform.annotation.RateLimiter;
import org.hjhy.homeworkplatform.constant.RoleConstant;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.ClassDto;
import org.hjhy.homeworkplatform.dto.ClazzConditionDto;
import org.hjhy.homeworkplatform.enums.LimitTypeEnum;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.UserClassRole;
import org.hjhy.homeworkplatform.generator.service.ClazzService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.ClassInfoVo;
import org.hjhy.homeworkplatform.vo.PageResult;
import org.hjhy.homeworkplatform.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author HJHY
 * @date 2023/1/9 11:47
 */
@Tag(name = "班级模块接口")
@RestController
public class ClazzController {
    private final ClazzService clazzService;

    public ClazzController(ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    //接口幂等性可以由数据库唯一索引约束来保证
    @Operation(summary = "创建班级", description = "创建班级接口描述")
    @PostMapping("/classes")
    public Result<?> saveClass(@Valid @RequestBody ClassDto classDto) {
        Clazz clazz = clazzService.createClass(RequestContext.getAuthInfo().getUserId(), classDto);
        return Result.ok(clazz);
    }

    @Operation(summary = "删除班级", description = "删除班级接口描述")
    @HasRole(roles = {RoleConstant.CLASS_CREATOR})
    @DeleteMapping("/classes/{classId}")
    public Result<?> deleteClass(@PathVariable Integer classId) {
        clazzService.deleteClass(classId);
        return Result.ok();
    }

    @Operation(summary = "更新班级", description = "更新班级接口描述")
    @HasRole(roles = {RoleConstant.CLASS_CREATOR})
    @PutMapping("/classes/{classId}")
    public Result<?> updateClass(@Valid @RequestBody ClassDto classDto,
                                 @PathVariable Integer classId) {
        clazzService.updateClass(classId, classDto);
        return Result.ok();
    }

    @Operation(summary = "查找班级", description = "查找班级接口描述")
    @HasRole
    @GetMapping("/classes/{classId}")
    public Result<Clazz> findClass(@PathVariable Integer classId) {
        var clazz = clazzService.findClass(classId);
        return Result.ok(clazz);
    }

    @Operation(summary = "查找班级详情", description = "查找班级详情接口描述")
    @HasRole
    @GetMapping("/classes/{classId}/detail")
    public Result<ClassInfoVo> detail(@PathVariable Integer classId) {
        var classInfoDtoList = clazzService.detail(classId);
        return Result.ok(classInfoDtoList);
    }

    //条件查询的权限检查由业务完成(因此没办法获取到classId,无法进行权限检查)
    @Operation(summary = "条件查询班级", description = "条件查询班级接口描述")
    @RateLimiter(limitType = LimitTypeEnum.USER_ID)
    @GetMapping(value = "/classes/condition")
    public Result<PageResult<Clazz>> condition(ClazzConditionDto clazzConditionDto,
                                               @RequestParam(required = false) Integer current,
                                               @RequestParam(required = false) Integer pageSize) {
        Page<Clazz> page = CommonUtils.getPage(current, pageSize);
        var classInfoDtoList = clazzService.condition(page, clazzConditionDto);
        return Result.ok(classInfoDtoList);
    }

    @Operation(summary = "查找班级简要信息", description = "查找班级简要信息接口描述")
    @GetMapping("/classes/simpleInfo")
    public Result<List<ClassInfoVo>> simpleInfo(@RequestParam(name = "classIdList") List<Integer> classIdList) {
        var classInfoDtoList = clazzService.simpleInfo(classIdList);
        return Result.ok(classInfoDtoList);
    }

    @Operation(summary = "查找创建的班级", description = "查找创建的班级")
    @GetMapping("/classes/created")
    public Result<PageResult<ClassInfoVo>> createdClasses(@RequestParam(required = false) Integer currentPage,
                                                          @RequestParam(required = false) Integer pageSize) throws InterruptedException {
        Page<Clazz> page = CommonUtils.getPage(currentPage, pageSize);
        var classInfoDtoList = clazzService.getCreatedClasses(RequestContext.getAuthInfo().getUserId(), page);
        return Result.ok(classInfoDtoList);
    }

    @Operation(summary = "查询加入的班级", description = "查询加入的班级")
    @GetMapping("/classes/joined")
    public Result<PageResult<ClassInfoVo>> joinedClasses(@RequestParam(required = false) Integer currentPage,
                                                         @RequestParam(required = false) Integer pageSize) {
        Page<UserClassRole> page = CommonUtils.getPage(currentPage, pageSize);
        var classInfoDtoList = clazzService.getJoinedClasses(RequestContext.getAuthInfo().getUserId(), page);
        return Result.ok(classInfoDtoList);
    }

    //通过加锁的方式保证接口幂等性
    @Operation(summary = "分享班级", description = "获取班级验证码")
    @HasRole(roles = {RoleConstant.CLASS_CREATOR, RoleConstant.CLASS_ADMIN})
    @GetMapping("/classes/{classId}/share")
    public Result<String> shareClass(@PathVariable Integer classId) {
        var shareCode = clazzService.shareClass(RequestContext.getAuthInfo().getUserId(), classId);
        return Result.ok(shareCode);
    }

    //使用数据库唯一索引保证接口幂等性
    @Operation(summary = "加入班级", description = "加入班级接口描述")
    @GetMapping("/classes/{shareCode}/join")
    public Result<?> joinClass(@PathVariable String shareCode) {
        clazzService.joinClass(RequestContext.getAuthInfo().getUserId(), shareCode);
        return Result.ok();
    }

    @Operation(summary = "退出班级", description = "退出班级接口描述")
    @HasRole
    @DeleteMapping("/classes/{classId}/exit")
    public Result<?> exitClass(@PathVariable Integer classId) {
        clazzService.exitClass(RequestContext.getAuthInfo().getUserId(), classId);
        return Result.ok();
    }

    @Operation(summary = "查看班级管理员", description = "查看班级管理员接口描述")
    @HasRole
    @GetMapping("/classes/{classId}/admin")
    public Result<List<Integer>> queryClassAdmin(@PathVariable Integer classId) {
        var list = clazzService.queryClassAdmin(classId);
        return Result.ok(list);
    }

    @Operation(summary = "设置班级管理员", description = "设置班级管理员接口描述")
    @HasRole(roles = {RoleConstant.CLASS_CREATOR})
    @PostMapping("/classes/{classId}/admin")
    public Result<?> setClassAdmin(@PathVariable Integer classId,
                                   @RequestParam("userIdList") List<Integer> userIdList) {
        clazzService.setClassAdmin(classId, userIdList);
        return Result.ok();
    }
}
