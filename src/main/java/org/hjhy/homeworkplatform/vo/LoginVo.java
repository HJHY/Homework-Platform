package org.hjhy.homeworkplatform.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author HJHY
 * @date 2024/2/6 10:28
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class LoginVo {
    /**
     * 用户token
     */
    private String accessToken;

    /**
     * 刷新token
     */
    private String refreshToken;
}
