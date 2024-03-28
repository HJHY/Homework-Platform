package org.hjhy.homeworkplatform.constant;

/**
 * @author HJHY
 * @date 2024/2/4 17:17
 */
public class RedisPrefixConst {
    /*登录验证码存储的前缀*/
    public static final String REGISTER_CAPTCHA_PREFIX = "register_captcha_prefix:";

    /*access-token存储的前缀*/
    public static final String ACCESS_TOKEN_PREFIX = "access-token:";

    /*refresh-token存储的前缀*/
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /*班级分享码在redis中存储的前缀(classId映射到sharedCode)*/
    public static final String CLASS_ID_TO_SHARE_CODE_PREFIX = "class_id_to_sharedcode_prefix:";

    public static final String SHARED_CODE_TO_CLASS_ID_PREFIX = "sharedcode_to_class_id_prefix:";
}
