package org.hjhy.homeworkplatform.generator.service.impl;

import org.hjhy.homeworkplatform.constant.GlobalConst;
import org.hjhy.homeworkplatform.generator.service.RedisService;
import org.hjhy.homeworkplatform.utils.CommonUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author HJHY
 * @date 2024/3/31 14:45
 */
@Service
public class RedisServiceImpl implements RedisService {
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getAndSetIdempotentToken(String key) {
        //生成幂等性token
        String idempotentToken = CommonUtils.generateIdempotentToken();
        //将幂等性token存入redis
        redisTemplate.opsForValue().set(key, idempotentToken, GlobalConst.IDEMPOTENT_TOKEN_EXPIRE_TIME, TimeUnit.MILLISECONDS);

        return idempotentToken;
    }
}
