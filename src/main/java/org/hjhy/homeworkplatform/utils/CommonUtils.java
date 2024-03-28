package org.hjhy.homeworkplatform.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import lombok.Getter;
import org.hjhy.homeworkplatform.constant.GlobalConst;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Random;

/**
 * @author HJHY
 * @date 2024/2/4 17:10
 */
public class CommonUtils {
    @Getter
    private static final ObjectMapper objectMapper;

    static {
        /*配置objectMapper*/
        objectMapper = new ObjectMapper();
        //反序列化时不匹配的值直接忽略不报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**
     * 生成验证码
     *
     * @param bits bits
     * @return str code
     */
    public static String generateCode(int bits) {
        if (bits < 0) {
            throw new RuntimeException("验证码生成的位数不能为负数");
        }
        var builder = new StringBuilder();
        var random = new Random();
        for (int i = 0; i < bits; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    public static Cookie getCookie(Cookie[] cookies, String name) {
        if (ObjectUtils.isEmpty(cookies)) {
            return null;
        }

        var cookie = Arrays.stream(cookies).filter(coo -> coo.getName().equals(name)).findFirst();
        return cookie.orElse(null);
    }

    public static <T> Page<T> getPage(Integer page, Integer size) {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = GlobalConst.DEFAULT_PAGE_SIZE;
        }

        return new Page<>(page, size);
    }

    /**
     * 计算时间差
     *
     * @param source 初始时间(小)
     * @param dest   目的之间(大)
     * @return 毫秒时间延迟
     */
    public static long getDelayTime(LocalDateTime source, LocalDateTime dest) {
        return dest.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - source.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
