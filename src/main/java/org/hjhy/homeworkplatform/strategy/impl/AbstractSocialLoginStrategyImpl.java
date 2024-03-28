package org.hjhy.homeworkplatform.strategy.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.dto.SocialTokenDTO;
import org.hjhy.homeworkplatform.dto.SocialUserInfoDto;
import org.hjhy.homeworkplatform.enums.LoginTypeEnum;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.SocialAuth;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.generator.service.SocialAuthService;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.strategy.SocialLoginStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 第三方登录抽象模板
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
public abstract class AbstractSocialLoginStrategyImpl implements SocialLoginStrategy {
    private final SocialAuthService socialAuthService;
    private final UserService userService;

    protected AbstractSocialLoginStrategyImpl(SocialAuthService socialAuthService, UserService userService) {
        this.socialAuthService = socialAuthService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public User login(SocialLoginBaseDto socialLoginBaseDto, LoginTypeEnum loginType) throws Exception {
        // 获取第三方token信息
        SocialTokenDTO socialToken = getSocialToken(socialLoginBaseDto);
        // 根据access token获取用户信息
        SocialUserInfoDto socialUserInfo = getSocialUserInfo(socialToken.getAccessToken());
        //判断用户是否已经注册
        SocialAuth userAuth = getUserAuth(socialUserInfo, loginType);

        User user;

        if (ObjectUtils.isEmpty(userAuth)) {
            //用户没有进行注册,则注册为新用户
            user = saveUser(socialUserInfo);
            SocialAuth socialAuth = SocialAuth.builder()
                    .openId(socialUserInfo.getOpenId())
                    .userId(user.getId())
                    .loginType(loginType.getType())
                    .build();
            socialAuthService.save(socialAuth);
        } else {
            //用户已经注册过了,查询用户信息
            user = userService.getById(userAuth.getUserId());
            //检查用户账号是否被禁用
            if (user.getIsValid().equals(0)) {
                throw new BaseException(StatusCode.USER_LOCKED);
            }
        }
        return user;
    }


    /**
     * 获取第三方access token和refresh token信息
     *
     * @param socialLoginBaseDto 数据
     * @return {@link SocialTokenDTO} 第三方token信息
     */
    public abstract SocialTokenDTO getSocialToken(SocialLoginBaseDto socialLoginBaseDto) throws Exception;

    /**
     * 根据access token获取用户信息
     * @param accessToken access token
     * @return SocialUserInfoDto 用户信息
     */
    public abstract SocialUserInfoDto getSocialUserInfo(String accessToken);

    /**
     * 获取用户账号
     *
     * @return {@link SocialAuth} 用户账号
     */
    private SocialAuth getUserAuth(SocialUserInfoDto socialUserInfoDto, LoginTypeEnum loginTypeEnum) {
        return socialAuthService.getOne(new LambdaQueryWrapper<SocialAuth>()
                .eq(SocialAuth::getOpenId, socialUserInfoDto.getOpenId())
                .eq(SocialAuth::getLoginType, loginTypeEnum.getType()));
    }


    /**
     * 根据查询出来的第三方角色信息保存新用户
     * @param socialUserInfo  socialUserInfo
     * @return user
     */
    private User saveUser(SocialUserInfoDto socialUserInfo) {
        User user = User.builder()
                .username(socialUserInfo.getName())
                .realname(socialUserInfo.getName())
                .email(socialUserInfo.getEmail())
                .iconUrl(socialUserInfo.getAvatarUrl())
                .isValid(1)
                .build();
        userService.save(user);
        return user;
    }
}
