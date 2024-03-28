package org.hjhy.homeworkplatform.config.aliyun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云函数计算配置类
 *
 * @author HJHY
 * @date 2024/2/27 14:58
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cloud-storage.aliyun.fc")
public class AliCloudFCConfigProperties {
    /**
     * 访问密钥id
     */
    private String accessKeyId;

    /**
     * 访问密钥密码
     */
    private String accessKeySecret;

    /**
     * regionId
     */
    private String regionId;

    /**
     * 函数计算访问地址
     */
    private String url;

    /**
     * bucket名称
     */
    private String bucketName;

    /**
     * 原始文件存储路径
     */
    private String sourceDir;
}
