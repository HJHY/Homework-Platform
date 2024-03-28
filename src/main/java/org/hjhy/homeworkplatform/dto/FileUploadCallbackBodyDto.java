package org.hjhy.homeworkplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * @author HJHY
 * @date 2024/3/19 10:32
 */
@NoArgsConstructor
public class FileUploadCallbackBodyDto {
    @AllArgsConstructor
    @Data
    @Builder
    @ToString
    public static class AliCloud {
        @NotBlank(message = "mimeType不能为空")
        String mimeType;
        @NotNull(message = "size不能为空")
        Integer size;
        @NotNull(message = "homeworkId不能为空")
        Integer homeworkId;
    }
}
