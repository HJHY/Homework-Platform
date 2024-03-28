package org.hjhy.homeworkplatform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社交登录token
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SocialTokenDTO {
    /**
     * 访问令牌
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 令牌类型
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * 过期时间
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * 刷新令牌
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * 作用域
     */
    @JsonProperty("scope")
    private String scope;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private Long createdAt;
}
