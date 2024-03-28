package org.hjhy.homeworkplatform.config.properties.login;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author HJHY
 * @date 2024/3/2 19:41
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "social-login.gitee")
public class GiteeLoginProperties {
    private String clientId;

    private String clientSecret;

    private String redirectUrl;

    private String oAuthUrI;

    private String userInfoUrI;
}
