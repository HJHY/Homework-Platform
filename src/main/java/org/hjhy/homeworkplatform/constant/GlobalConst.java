package org.hjhy.homeworkplatform.constant;

import org.springframework.context.annotation.Configuration;

/**
 * @author HJHY
 * @date 2023/1/7 16:20
 */
@Configuration
public class GlobalConst {
    public static final String EMAIL_REGEX = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    /**
     * 默认的分页条数
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;
}
