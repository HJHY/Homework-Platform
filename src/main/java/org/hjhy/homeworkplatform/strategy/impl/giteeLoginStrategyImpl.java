package org.hjhy.homeworkplatform.strategy.impl;

import com.aliyun.tea.okhttp.OkHttpClientBuilder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.HttpStatus;
import org.hjhy.homeworkplatform.config.properties.login.GiteeLoginProperties;
import org.hjhy.homeworkplatform.constant.SocialLoginConstant;
import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.dto.SocialTokenDTO;
import org.hjhy.homeworkplatform.dto.SocialUserInfoDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.generator.service.SocialAuthService;
import org.hjhy.homeworkplatform.generator.service.UserService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * 微博登录策略实现
 *
 * @author hjhy
 * @date 2024/02/28
 */
@Service("giteeLoginStrategyImpl")
@Slf4j
public class giteeLoginStrategyImpl extends AbstractSocialLoginStrategyImpl {
    private final GiteeLoginProperties giteeLoginProperties;

    protected giteeLoginStrategyImpl(SocialAuthService socialAuthService, UserService userService, GiteeLoginProperties giteeLoginProperties) {
        super(socialAuthService, userService);
        this.giteeLoginProperties = giteeLoginProperties;
    }

    @Override
    public SocialTokenDTO getSocialToken(SocialLoginBaseDto socialLoginBaseDto) throws Exception {
        OkHttpClient okHttpClient = new OkHttpClientBuilder().buildOkHttpClient();
        HashMap<String, String> param = new HashMap<>(5);
        param.put(SocialLoginConstant.Gitee.GRANT_TYPE, SocialLoginConstant.Gitee.LOGIN_GRANT_TYPE);
        param.put(SocialLoginConstant.Gitee.CODE, socialLoginBaseDto.getCode());
        param.put(SocialLoginConstant.Gitee.CLIENT_ID, giteeLoginProperties.getClientId());
        param.put(SocialLoginConstant.Gitee.CLIENT_SECRET, giteeLoginProperties.getClientSecret());
        param.put(SocialLoginConstant.Gitee.REDIRECT_URI, giteeLoginProperties.getRedirectUrl());

        var jsonParams = CommonUtils.getObjectMapper().writeValueAsString(param);

        Request request = new Request.Builder().url(giteeLoginProperties.getOAuthUrI()).post(RequestBody.create(jsonParams, MediaType.parse("application/json"))).build();

        SocialTokenDTO socialTokenDTO;
        try (Response res = okHttpClient.newCall(request).execute()) {
            if (res.code() != HttpStatus.SC_OK) {
                log.error("res.code={},获取gitee token失败", res.code());
                throw new BaseException("获取gitee access token失败");
            }
            //从json字符串中读取属性
            var body = res.body();
            if (body == null) {
                throw new IOException("access token请求返回体为空");
            }
            socialTokenDTO = CommonUtils.getObjectMapper().readValue(body.string(), SocialTokenDTO.class);
        } catch (IOException e) {
            log.error("获取gitee user info失败", e);
            throw new BaseException("获取gitee access token失败");
        }

        return socialTokenDTO;
    }

    @Override
    public SocialUserInfoDto getSocialUserInfo(String accessToken) {
        OkHttpClient okHttpClient = new OkHttpClientBuilder().buildOkHttpClient();
        HttpUrl paramUrl = Objects.requireNonNull(HttpUrl.parse(giteeLoginProperties.getUserInfoUrI()))
                .newBuilder()
                .addQueryParameter(SocialLoginConstant.Gitee.ACCESS_TOKEN, accessToken)
                .build();
        Request request = new Request.Builder().url(paramUrl).get().build();
        SocialUserInfoDto socialUserInfoDto;
        try (Response res = okHttpClient.newCall(request).execute()) {
            if (res.code() != HttpStatus.SC_OK) {
                log.error("获取gitee用户信息失败");
                throw new BaseException("获取gitee user info失败");
            }
            //从json字符串中读取属性
            var body = res.body();
            if (body == null) {
                throw new IOException("获取gitee用户信息请求返回体为空");
            }
            socialUserInfoDto = CommonUtils.getObjectMapper().readValue(body.string(), SocialUserInfoDto.class);
        } catch (IOException e) {
            log.error("获取gitee用户信息调用出现异常", e);
            throw new BaseException("获取gitee用户信息调用出现异常");
        }
        return socialUserInfoDto;
    }
}
