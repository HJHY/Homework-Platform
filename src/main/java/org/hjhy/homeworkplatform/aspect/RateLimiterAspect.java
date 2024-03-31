package org.hjhy.homeworkplatform.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.hjhy.homeworkplatform.annotation.RateLimiter;
import org.hjhy.homeworkplatform.annotation.RateRule;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.context.RequestContext;
import org.hjhy.homeworkplatform.dto.AuthInfoDto;
import org.hjhy.homeworkplatform.enums.LimitTypeEnum;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.utils.IpUtils;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


/**
 * @author HJHY
 * @date 2024/3/31 15:11
 */
@Aspect
@Component
@Slf4j
@Order(0)
public class RateLimiterAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimiterAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 通过 rateLimiter 和 joinPoint 拼接  prefix : ip / userId : classSimpleName - methodName
     * KEYS[1] = prefix : ? : className : methodName
     * KEYS[2] = 唯一ID
     * KEYS[3] = 当前时间
     * ARGV = [次数，单位时间，次数，单位时间, 次数, 单位时间 ...]
     * @param rateLimiter 提供 prefix
     * @param joinPoint   提供 classSimpleName : methodName
     * @return rate limit key
     */
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint joinPoint) {
        StringBuilder key = new StringBuilder(rateLimiter.key());
        // 不同限流类型使用不同的前缀
        switch (rateLimiter.limitType()) {
            case LimitTypeEnum.IP:
                HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
                String ipAddress = IpUtils.getIpAddress(request);
                key.append(LimitTypeEnum.IP.getTypeName()).append("{");
                key.append(ipAddress);
                key.append("}").append(":");
                break;
            case LimitTypeEnum.USER_ID:
                AuthInfoDto authInfo = RequestContext.getAuthInfo();
                if (ObjectUtils.isEmpty(authInfo)) {
                    log.error("进行限流时获取用户信息失败,请检查接口是否需要提供用户信息");
                    throw new BaseException("限流功能失败");
                }
                key.append(LimitTypeEnum.USER_ID.getTypeName()).append("{");
                key.append(authInfo.getUserId());
                key.append("}").append(":");
                break;
            case LimitTypeEnum.ClASS_ID:
                Integer classId = RequestContext.getClassId();
                if (ObjectUtils.isEmpty(classId)) {
                    log.error("进行限流时获取班级信息失败,请检查接口是否需要提供班级信息");
                    throw new BaseException("限流功能失败");
                }
                key.append(LimitTypeEnum.ClASS_ID.getTypeName()).append("{");
                key.append(classId);
                key.append("}").append(":");
                break;
            case LimitTypeEnum.HOMEWORK_ID:
                Integer homeworkId = RequestContext.getHomeworkId();
                if (ObjectUtils.isEmpty(homeworkId)) {
                    log.error("进行限流时获取作业信息失败,请检查接口是否需要提供作业信息");
                    throw new BaseException("限流功能失败");
                }
                key.append(LimitTypeEnum.HOMEWORK_ID.getTypeName()).append("{");
                key.append(homeworkId);
                key.append("}").append(":");
                break;
            case GLOBAL:
                key.append(LimitTypeEnum.GLOBAL.getTypeName()).append(":");
                break;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        key.append(targetClass.getSimpleName()).append("-").append(method.getName());
        return key.toString();
    }

    private RedisScript<Long> getLimitScript() {
        String script = "local key = KEYS[1];" +
                "local uuid = KEYS[2];" +
                "local currentTime = tonumber(KEYS[3]);" +
                "local expireTime = -1;" +
                "for i = 1, #ARGV, 2 do" +
                "    local rateRuleCount = tonumber(ARGV[i]);" +
                "    local rateRuleTime = tonumber(ARGV[i + 1]);" +
                "    local count = redis.call('ZCOUNT', key, currentTime - rateRuleTime, currentTime);" +
                "    if tonumber(count) >= rateRuleCount then" +
                "        return 1;" +
                "    end;" +
                "    if rateRuleTime > expireTime then" +
                "        expireTime = rateRuleTime;" +
                "    end;" +
                "end;" +
                "redis.call('ZADD', key, currentTime, uuid);" +
                /*"-- 5. 更新缓存过期时间" +*/
                "redis.call('PEXPIRE', key, expireTime);" +
                /*"-- 6. 删除最大时间限度之前的数据，防止数据过多" +*/
                "redis.call('ZREMRANGEBYSCORE', key, 0, currentTime - expireTime);" +
                "return 0;";

        return new DefaultRedisScript<>(script, Long.class);
    }

    /**
     * 获取规则
     *
     * @param rateLimiter 获取其中规则信息
     * @return Long[]
     */
    private Long[] getRules(RateLimiter rateLimiter) {
        int capacity = rateLimiter.rules().length << 1;
        // 1. 构建 args
        Long[] args = new Long[rateLimiter.preventDuplicate() ? capacity + 2 : capacity];
        // 3. 记录数组元素
        int index = 0;
        // 2. 判断是否需要添加防重复提交到redis进行校验
        if (rateLimiter.preventDuplicate()) {
            RateRule preventRateRule = rateLimiter.preventDuplicateRule();
            args[index++] = preventRateRule.count();
            args[index++] = preventRateRule.timeUnit().toMillis(preventRateRule.time());
        }
        RateRule[] rules = rateLimiter.rules();
        for (RateRule rule : rules) {
            args[index++] = rule.count();
            args[index++] = rule.timeUnit().toMillis(rule.time());
        }
        return args;
    }

    /**
     * 限流
     * XXX 对限流要求比较高，可以使用在 Redis中对规则进行存储校验 或者使用中间件
     *
     * @param joinPoint   joinPoint
     * @param rateLimiter 限流注解
     */
    @Before(value = "@annotation(rateLimiter)")
    public void boBefore(JoinPoint joinPoint, RateLimiter rateLimiter) {
        log.info("进行限流检查");
        // 1. 生成 key
        String key = getCombineKey(rateLimiter, joinPoint);
        // 2. 执行脚本返回是否限流
        Long res = redisTemplate.execute(getLimitScript(),
                List.of(key, UUID.randomUUID().toString().replace("-", ""), String.valueOf(System.currentTimeMillis())),
                (Object[]) getRules(rateLimiter));
        // 3. 判断限流接口
        if (ObjectUtils.isEmpty(res)) {
            log.error("限流失败");
            throw new BaseException("限流失败");
        }

        if (res == 1) {
            log.warn("请求过于频繁，请稍后再试");
            throw new BaseException(StatusCode.REQUEST_FREQUENTLY);
        }
    }
}
