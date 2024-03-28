package org.hjhy.homeworkplatform.config.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author HJHY
 * @date 2024/2/21 21:30
 */
@Configuration
public class AliCloudConfig {
    private final OssUploadConfigProperties ossConfigProperties;
    private final AliCloudFCConfigProperties aliCloudFCConfigProperties;

    public AliCloudConfig(OssUploadConfigProperties ossConfigProperties, AliCloudFCConfigProperties aliCloudFCConfigProperties) {
        this.ossConfigProperties = ossConfigProperties;
        this.aliCloudFCConfigProperties = aliCloudFCConfigProperties;
    }

    @Bean(name = "ossClient")
    public OSS getOssClient() {
        return new OSSClientBuilder().build(ossConfigProperties.getEndpoint(), ossConfigProperties.getAccessKeyId(), ossConfigProperties.getAccessKeySecret());
    }

    @Bean(name = "fcClient")
    public com.aliyun.fc_open20210406.Client getFcClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(aliCloudFCConfigProperties.getAccessKeyId())
                .setAccessKeySecret(aliCloudFCConfigProperties.getAccessKeySecret())
                .setRegionId(aliCloudFCConfigProperties.getRegionId());
        return new com.aliyun.fc_open20210406.Client(config);
    }

}
