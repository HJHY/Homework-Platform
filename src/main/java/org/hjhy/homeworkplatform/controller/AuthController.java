package org.hjhy.homeworkplatform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hjhy.homeworkplatform.constant.GlobalConst;
import org.hjhy.homeworkplatform.dto.LoginDto;
import org.hjhy.homeworkplatform.dto.RegisterDto;
import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.enums.LoginTypeEnum;
import org.hjhy.homeworkplatform.generator.service.AuthService;
import org.hjhy.homeworkplatform.vo.LoginVo;
import org.hjhy.homeworkplatform.vo.Result;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证模块接口")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "获取验证码", description = "用户向服务器获取登录验证码")
    @GetMapping("/code")
    public Result<?> sendCaptcha(@Email(message = "邮件格式不合法", regexp = GlobalConst.EMAIL_REGEX)
                                 @NotBlank(message = "邮件格式不能为空")
                                 @RequestParam(value = "email") String email) {
        String captcha = authService.sendCaptcha(email);
        return Result.ok(captcha);
    }

    @Operation(summary = "注册", description = "新用户登录系统前需要先进行用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDto registerDto) {
        authService.register(registerDto);
        return Result.ok();
    }

    @Operation(summary = "登录", description = "用户登录")
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        return authService.login(loginDto, response);
    }

    @Operation(summary = "注销", description = "用户注销")
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "刷新token", description = "使用refreshToken刷新")
    @PostMapping("/refreshToken")
    public Result<?> refreshToken(@RequestHeader(value = "RefreshToken") String refreshToken, HttpServletResponse response) {
        var result = authService.refreshToken(refreshToken, response);
        return Result.ok(result);
    }

    @PostMapping("/social/login/gitee")
    public Result<LoginVo> socialLogin(@Valid @RequestBody SocialLoginBaseDto socialLoginBaseDto,
                                       HttpServletResponse response) throws Exception {
        LoginVo loginVo = authService.socialLogin(socialLoginBaseDto, LoginTypeEnum.GITEE.getMode(), response);
        return Result.ok(loginVo);
    }
}