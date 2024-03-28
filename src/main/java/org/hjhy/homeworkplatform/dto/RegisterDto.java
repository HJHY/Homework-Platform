package org.hjhy.homeworkplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hjhy.homeworkplatform.constant.GlobalConst;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class RegisterDto {
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "邮件格式不合法", regexp = GlobalConst.EMAIL_REGEX)
    @NotBlank(message = "邮件格式不能为空")
    String email;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    String username;

    @Schema(description = "真实姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "真实姓名不能为空")
    String realName;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    String password;

    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "验证码不能为空")
    String captcha;
}
