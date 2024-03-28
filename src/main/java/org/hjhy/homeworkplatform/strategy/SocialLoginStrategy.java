package org.hjhy.homeworkplatform.strategy;

import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.enums.LoginTypeEnum;
import org.hjhy.homeworkplatform.generator.domain.User;

/**
 * 第三方登录策略
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
public interface SocialLoginStrategy {

    /**
     * 登录
     *
     * @param socialLoginBaseDto 数据
     * @return {@link User} 用户信息
     */
    User login(SocialLoginBaseDto socialLoginBaseDto, LoginTypeEnum loginType) throws Exception;
}
