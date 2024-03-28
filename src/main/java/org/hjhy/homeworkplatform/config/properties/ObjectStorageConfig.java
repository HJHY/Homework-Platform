package org.hjhy.homeworkplatform.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author HJHY
 * @date 2024/2/29 21:43
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "cloud-storage")
public class ObjectStorageConfig {
    String mode;
}
