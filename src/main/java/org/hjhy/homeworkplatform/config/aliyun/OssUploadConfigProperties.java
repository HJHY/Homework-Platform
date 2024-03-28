package org.hjhy.homeworkplatform.config.aliyun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * oss配置属性
 *
 * @author yezhiqiu
 * @date 2021/07/28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cloud-storage.aliyun.oss")
public class OssUploadConfigProperties {

    /**
     * oss域名
     */
    private String url;

    /**
     * 终点
     */
    private String endpoint;

    /**
     * 访问密钥id
     */
    private String accessKeyId;

    /**
     * 访问密钥密码
     */
    private String accessKeySecret;

    /**
     * bucket名称
     */
    private String bucketName;

    /**
     * 作业文件上传后的回调地址
     */
    private String callbackUrl;
}
