package org.hjhy.homeworkplatform.generator.service;

import jakarta.servlet.http.HttpServletResponse;
import org.hjhy.homeworkplatform.dto.AuthInfoDto;
import org.hjhy.homeworkplatform.dto.LoginDto;
import org.hjhy.homeworkplatform.dto.RegisterDto;
import org.hjhy.homeworkplatform.dto.SocialLoginBaseDto;
import org.hjhy.homeworkplatform.vo.LoginVo;
import org.hjhy.homeworkplatform.vo.Result;

/**
 * @author HJHY
 * @date 2024/2/4 15:00
 */
public interface AuthService {
    AuthInfoDto getAuthInfoFromAccessToken(String accessToken);

    String getAccessTokenFromRefreshToken(String accessToken);

    void deleteAccessToken(String accessToken);

    void deleteRefreshToken(String refreshToken);


    /*------------------------------------------下面是接口相关的service---------------------------------------------------*/
    String sendCaptcha(String email);

    void register(RegisterDto registerDto);

    Result<?> login(LoginDto loginDto, HttpServletResponse response);

    void logout();

    Result<LoginVo> refreshToken(String refreshToken, HttpServletResponse response);

    LoginVo socialLogin(SocialLoginBaseDto socialLoginBaseDto, String mode, HttpServletResponse response) throws Exception;
}
