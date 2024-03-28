package org.hjhy.homeworkplatform.vo;

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
    private String accessid;

    // 策略
    private String policy;

    // 签名
    private String signature;

    // 文件路径(等同于全文件名key,也就是OSS的Object Name)
    private String filePath;

    // OSS访问地址
    private String host;

    // 过期时间
    private String expire;

    //经过Base64编码的回调地址请求体
    private String callback;
}
