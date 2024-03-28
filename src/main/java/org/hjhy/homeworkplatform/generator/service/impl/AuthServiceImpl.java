package org.hjhy.homeworkplatform.generator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.AuthConstant;
import org.hjhy.homeworkplatform.constant.RedisPrefixConst;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.context.SocialLoginStrategyContext;
import org.hjhy.homeworkplatform.dto.*;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.service.AuthService;
import org.hjhy.homeworkplatform.generator.service.MessageService;
import org.hjhy.homeworkplatform.generator.service.PushSettingService;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.hjhy.homeworkplatform.vo.LoginVo;
import org.hjhy.homeworkplatform.vo.Result;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

import static org.hjhy.homeworkplatform.constant.StatusCode.*;

/**
 * @author HJHY
 * @date 2024/2/4 15:01
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    /*验证码长度*/
    private static final Integer CAPTCHA_BIT = 6;

    /*登录验证码的有效时间*/
    private static final Integer CAPTCHA_AVAIL_DURATION_MINUTE = 10;

    /*accessToken的有效时间*/
    private static final Integer ACCESS_TOKEN_AVAIL_DURATION_DAY = 3;

    /*refreshToken的有效时间*/
    private static final Integer REFRESH_TOKEN_AVAIL_DURATION_DAY = 7;

    private final RedisTemplate<String, Object> redisTemplate;
    private final MessageService messageService;
    private final UserService userService;
    private final PushSettingService pushSettingService;
    private final SocialLoginStrategyContext socialLoginStrategyContext;

    public AuthServiceImpl(RedisTemplate<String, Object> redisTemplate, MessageService messageService, UserService userService, PushSettingService pushSettingService, SocialLoginStrategyContext socialLoginStrategyContext) {
        this.redisTemplate = redisTemplate;
        this.messageService = messageService;
        this.userService = userService;
        this.pushSettingService = pushSettingService;
        this.socialLoginStrategyContext = socialLoginStrategyContext;
    }


    @Override
    public String sendCaptcha(String email) {
        //检查用户是否已经存在(存在则不需要进行注册)
        if (userService.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email).eq(User::getIsValid, true))) {
            log.warn("用户已存在");
            throw new BaseException(StatusCode.USER_ALREADY_EXISTED);
        }
        //生成验证码
        var captcha = CommonUtils.generateCode(CAPTCHA_BIT);
        log.info("生成验证码完成");
        //将验证码和email进行绑定
        redisTemplate.opsForValue().set(RedisPrefixConst.REGISTER_CAPTCHA_PREFIX + email, captcha, CAPTCHA_AVAIL_DURATION_MINUTE, TimeUnit.MINUTES);
        log.info("将验证码和邮箱的绑定关系设置到redis");
        //发送邮箱信息通知
        var emailDto = EmailDto.builder().toEmail(email).subject("作业收集平台注册信息").content("""
                    尊敬的用户:
                您本次的验证码为: %s,该验证码%d分钟内有效,请保护好验证码并及时进行操作.""".formatted(captcha, CAPTCHA_AVAIL_DURATION_MINUTE)).build();
        messageService.sendEmail(emailDto);
        log.info("将验证码信息推送至消息队列等待发送");

        return captcha;
    }

    @Override
    @Transactional
    public void register(RegisterDto registerDto) {
        log.info("用户注册开始，registerDto: {}", registerDto);
        //获取验证码
        var captcha = (String) redisTemplate.opsForValue().get(RedisPrefixConst.REGISTER_CAPTCHA_PREFIX + registerDto.getEmail());
        if (ObjectUtils.isEmpty(captcha)) {
            log.warn("用户注册时验证码不存在");
            throw new BaseException(NO_REGISTER_CAPTCHA);
        }
        //校验验证码
        if (!captcha.equals(registerDto.getCaptcha())) {
            log.warn("用户注册时验证码不匹配");
            throw new BaseException(REGISTER_CAPTCHA_NOT_MATCHED);
        }
        //删除验证码
        redisTemplate.delete(RedisPrefixConst.REGISTER_CAPTCHA_PREFIX + registerDto.getEmail());
        log.info("验证通过,删除验证码完成");

        //查询用户的存在性
        if (userService.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, registerDto.getEmail()).eq(User::getIsValid, true))) {
            log.warn("用户已存在");
            throw new BaseException(StatusCode.USER_ALREADY_EXISTED);
        }

        //新增用户
        var user = User.builder().email(registerDto.getEmail()).username(registerDto.getUsername()).realname(registerDto.getRealName()).password(BCrypt.hashpw(registerDto.getPassword(), BCrypt.gensalt())).build();
        userService.save(user);
        log.info("新增用户完成");
        //设置默认的DDL提醒时间
        pushSettingService.setDefaultDdls(user.getId());
    }

    @Override
    public Result<?> login(LoginDto loginDto, HttpServletResponse response) {
        //判断用户是否存在
        var user = userService.getOne(new LambdaQueryWrapper<User>().select(User::getId, User::getEmail, User::getPassword, User::getIsValid).eq(User::getEmail, loginDto.getEmail()).eq(User::getIsValid, true));
        if (ObjectUtils.isEmpty(user)) {
            log.warn("用户不存在");
            throw new BaseException(StatusCode.USER_NOT_EXIST);
        }

        //如果是通过第三方服务注册的用户如果存在邮箱的前提下,密码可能是NULL
        if (ObjectUtils.isEmpty(user.getPassword())) {
            log.warn("用户密码为空");
            throw new BaseException(StatusCode.USER_PASSWORD_NOT_SEATED);
        }

        //判断账号密码是否正确
        if (!BCrypt.checkpw(loginDto.getPassword(), user.getPassword())) {
            log.warn("账号密码不匹配");
            throw new BaseException(StatusCode.MISMATCH_PASSWORD);
        }
        //判断用户是否被封禁
        if (user.getIsValid().equals(0)) {
            log.warn("用户已经被封禁");
            throw new BaseException(USER_LOCKED);
        }

        //设置token到redis中
        var tokenVo = generateToken();
        setToken(tokenVo.getAccessToken(), tokenVo.getRefreshToken(), new AuthInfoDto(user.getId(), tokenVo.getAccessToken(), tokenVo.getRefreshToken()));
        //设置cookie
        setTokenCookie(tokenVo.getAccessToken(), response);
        return Result.ok(tokenVo);
    }

    private void setTokenCookie(String accessToken, HttpServletResponse response) {
        var cookie = new Cookie(AuthConstant.ACCESS_TOKEN_NAME, accessToken);
        cookie.setMaxAge(ACCESS_TOKEN_AVAIL_DURATION_DAY * 24 * 60 * 60);
        cookie.setPath("/");
        response.addCookie(cookie);
    }


    private LoginVo generateToken() {
        var accessToken = CommonUtils.generateCode(32);
        var refreshToken = CommonUtils.generateCode(32);
        return new LoginVo(accessToken, refreshToken);
    }

    public void setToken(String accessToken, String refreshToken, AuthInfoDto authInfoDto) {
        setAccessToken(accessToken, authInfoDto);
        setRefreshToken(refreshToken, accessToken);
    }

    private void setAccessToken(String accessToken, AuthInfoDto authInfoDto) {
        //access token (accessToken:{authInfoDto})
        redisTemplate.opsForValue().set(RedisPrefixConst.ACCESS_TOKEN_PREFIX + accessToken, authInfoDto, ACCESS_TOKEN_AVAIL_DURATION_DAY, TimeUnit.DAYS);
    }

    public AuthInfoDto getAuthInfoFromAccessToken(String accessToken) {
        return (AuthInfoDto) redisTemplate.opsForValue().get(RedisPrefixConst.ACCESS_TOKEN_PREFIX + accessToken);
    }

    private void setRefreshToken(String refreshToken, String accessToken) {
        //refresh token (refreshToken:{accessToken})
        redisTemplate.opsForValue().set(RedisPrefixConst.REFRESH_TOKEN_PREFIX + refreshToken, accessToken, REFRESH_TOKEN_AVAIL_DURATION_DAY, TimeUnit.DAYS);
    }

    public String getAccessTokenFromRefreshToken(String refreshToken) {
        return (String) redisTemplate.opsForValue().get(RedisPrefixConst.REFRESH_TOKEN_PREFIX + refreshToken);
    }

    @Override
    public void logout() {
        var authInfo = RequestContext.getAuthInfo();
        log.info("用户注销开始:{}", authInfo);
        //删除access token
        deleteAccessToken(authInfo.getAccessToken());
        log.info("删除access token完成");
        //删除refresh token
        if (!ObjectUtils.isEmpty(authInfo)) {
            log.info("删除refresh token完成");
            redisTemplate.delete(RedisPrefixConst.REFRESH_TOKEN_PREFIX + authInfo.getRefreshToken());
        }
    }

    public void deleteAccessToken(String accessToken) {
        redisTemplate.delete(RedisPrefixConst.ACCESS_TOKEN_PREFIX + accessToken);
    }

    public void deleteRefreshToken(String refreshToken) {
        redisTemplate.delete(RedisPrefixConst.REFRESH_TOKEN_PREFIX + refreshToken);
    }

    @Override
    public Result<LoginVo> refreshToken(String refreshToken, HttpServletResponse response) {
        log.info("用户刷新token开始, refreshToken: {}", refreshToken);
        //检查refresh token是否为但当前用户的
        var userAuth = RequestContext.getAuthInfo();
        if (ObjectUtils.isEmpty(getAccessTokenFromRefreshToken(refreshToken))) {
            throw new BaseException(REFRESH_TOKEN_NOT_EXIST);
        }

        if (!userAuth.getRefreshToken().equals(refreshToken)) {
            //token泄露删除refresh token和access token
            log.warn("refresh token泄露,将删除refresh token并抛出异常");
            deleteAccessToken(userAuth.getAccessToken());
            deleteRefreshToken(refreshToken);
            throw new BaseException(REFRESH_TOKEN_LEAKED);
        }

        deleteAccessToken(userAuth.getAccessToken());
        deleteRefreshToken(refreshToken);
        //生成新的token
        var tokenVo = generateToken();
        setToken(tokenVo.getAccessToken(), tokenVo.getRefreshToken(), new AuthInfoDto(userAuth.getUserId(), tokenVo.getAccessToken(), tokenVo.getRefreshToken()));
        setTokenCookie(tokenVo.getAccessToken(), response);
        return Result.ok(tokenVo);
    }

    @Override
    public LoginVo socialLogin(SocialLoginBaseDto socialLoginBaseDto, String mode, HttpServletResponse response) throws Exception {
        User user = socialLoginStrategyContext.executeLoginStrategy(socialLoginBaseDto, mode);
        //设置token到redis中
        var tokenVo = generateToken();
        setToken(tokenVo.getAccessToken(), tokenVo.getRefreshToken(), new AuthInfoDto(user.getId(), tokenVo.getAccessToken(), tokenVo.getRefreshToken()));
        //设置cookie
        setTokenCookie(tokenVo.getAccessToken(), response);
        return tokenVo;
    }
}
