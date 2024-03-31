package org.hjhy.homeworkplatform.generator.service;

/**
 * @author HJHY
 * @date 2024/3/31 14:43
 */
public interface RedisService {
    /**
     * 为接口幂等性功能提供接口
     * @param key  幂等性token的key
     * @return token
     */
    String getAndSetIdempotentToken(String key);
}
