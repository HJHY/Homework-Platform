package org.hjhy.homeworkplatform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.PasswordDto;
import org.hjhy.homeworkplatform.dto.UserDto;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.vo.Result;
import org.hjhy.homeworkplatform.vo.SimpleUserVo;
import org.hjhy.homeworkplatform.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author HJHY
 * @date 2023/1/6 22:55
 */
@Tag(name = "用户模块接口")
@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "查询用户详细信息", description = "查询用户详细信息")
    @GetMapping("/users")
    public Result<UserVo> info() {
        return Result.ok(userService.queryUserInfo(RequestContext.getAuthInfo().getUserId()));
    }

    @Operation(summary = "修改用户信息", description = "修改用户信息")
    @PutMapping("/users")
    public Result<?> updateInfo(@Valid @RequestBody UserDto userDto) {
        var user = User.builder().id(RequestContext.getAuthInfo().getUserId()).build();
        BeanUtils.copyProperties(userDto, user);
        userService.updateUserInfo(user);
        return Result.ok("用户信息修改成功");
    }

    @Operation(summary = "修改密码", description = "修改密码")
    @PutMapping("/users/password")
    public Result<?> updatePassword(@Valid @RequestBody PasswordDto passwordDto) {
        userService.updatePassword(RequestContext.getAuthInfo().getUserId(), passwordDto);
        return Result.ok("密码修改成功");
    }

    @Operation(summary = "查询多个用户简单信息", description = "查询用户简单信息")
    @GetMapping("/users/batch")
    public Result<List<SimpleUserVo>> simpleInfo(@RequestParam List<Integer> userIdList) {
        //查询多个用户的信息
        var simpleUserVoList = userService.queryUsers(userIdList);
        return Result.ok(simpleUserVoList);
    }

}