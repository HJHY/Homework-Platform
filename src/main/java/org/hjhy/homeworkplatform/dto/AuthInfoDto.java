package org.hjhy.homeworkplatform.dto;

import lombok.*;

/**
 * @author HJHY
 * @date 2024/2/6 11:27
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
@Builder
public class AuthInfoDto {
    Integer userId;

    String accessToken;

    String refreshToken;
}
