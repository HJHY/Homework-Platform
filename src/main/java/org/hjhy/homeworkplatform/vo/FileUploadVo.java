package org.hjhy.homeworkplatform.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * @author HJHY
 * @date 2024/2/26 16:36
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class FileUploadVo {
    // OSS访问ID
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String accessid;

    // 策略
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String policy;

    // 签名
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String signature;

    // 文件路径(等同于全文件名key,也就是OSS的Object Name)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String filePath;

    // OSS访问地址
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String host;

    // 过期时间
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String expire;

    //经过Base64编码的回调地址请求体
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String callback;

    // 幂等性token
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String idempotentToken;
}
