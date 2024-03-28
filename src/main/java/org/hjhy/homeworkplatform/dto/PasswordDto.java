package org.hjhy.homeworkplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author HJHY
 * @date 2023/1/8 17:34
 */
@AllArgsConstructor
@Data
public class PasswordDto {
    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始密码不能为空")
    String oldPassword;
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    String newPassword;
}
