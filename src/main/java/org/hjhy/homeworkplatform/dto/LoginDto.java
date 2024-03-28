package org.hjhy.homeworkplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hjhy.homeworkplatform.constant.GlobalConst;

/**
 * @author HJHY
 * @date 2023/1/7 22:32
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class LoginDto {

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮件格式不能为空")
    @Email(message = "邮件格式不合法", regexp = GlobalConst.EMAIL_REGEX)
    private String email;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;
}
