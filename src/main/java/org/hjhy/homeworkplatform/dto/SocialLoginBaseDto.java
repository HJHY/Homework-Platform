package org.hjhy.homeworkplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author HJHY
 * @date 2024/3/2 19:51
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class SocialLoginBaseDto {
    /**
     * code
     */
    @NotBlank(message = "code不能为空")
    private String code;
}
