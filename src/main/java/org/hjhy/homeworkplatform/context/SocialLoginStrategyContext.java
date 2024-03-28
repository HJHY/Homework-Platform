package org.hjhy.homeworkplatform.context;

import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.enums.LoginTypeEnum;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.domain.User;
import org.hjhy.homeworkplatform.strategy.SocialLoginStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * 第三方登录策略上下文
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Service
@Slf4j
public class SocialLoginStrategyContext {

    private final Map<String, SocialLoginStrategy> socialLoginStrategyMap;

    public SocialLoginStrategyContext(Map<String, SocialLoginStrategy> socialLoginStrategyMap) {
        this.socialLoginStrategyMap = socialLoginStrategyMap;
    }

    private LoginTypeEnum getLoginTypeEnum(String mode) {
        LoginTypeEnum strategyEnum = LoginTypeEnum.getStrategyEnum(mode);
        if (ObjectUtils.isEmpty(strategyEnum)) {
            log.error("社交登录策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        return strategyEnum;
    }

    /**
     * 执行第三方登录策略
     *
     * @param socialLoginBaseDto          数据
     * @param mode 模式
     * @return {@link User} 用户信息
     */
    public User executeLoginStrategy(SocialLoginBaseDto socialLoginBaseDto, String mode) throws Exception {
        LoginTypeEnum strategyEnum = getLoginTypeEnum(mode);
        return socialLoginStrategyMap.get(strategyEnum.getStrategy()).login(socialLoginBaseDto, strategyEnum);
    }
}
