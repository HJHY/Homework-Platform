package org.hjhy.homeworkplatform.constant;

/**
 * @author HJHY
 * @date 2024/2/4 17:17
 */
public class RedisPrefixConst {
    /*接口幂等性前缀*/
    public static final String IDEMPOTENT_TOKEN_PREFIX = "idempotent_token:";

    /*限流前缀*/
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /*************************************业务相关前缀*************************************/
    /*登录验证码存储的前缀*/
    public static final String REGISTER_CAPTCHA_PREFIX = "register_captcha_prefix:";

    /*access-token存储的前缀*/
    public static final String ACCESS_TOKEN_PREFIX = "access-token:";

    /*refresh-token存储的前缀*/
    public static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /*班级分享码生成时分布式锁的前缀*/
    public static final String CLASS_SHARED_CODE_LOCK_PREFIX = "class_shared_code_lock_prefix:";

    /*班级分享码在redis中存储的前缀(classId映射到sharedCode)*/
    public static final String CLASS_ID_TO_SHARE_CODE_PREFIX = "class_id_to_sharedcode_prefix:";

    /*班级分享码在redis中存在的前缀(sharedCode映射到classId)*/
    public static final String SHARED_CODE_TO_CLASS_ID_PREFIX = "sharedcode_to_class_id_prefix:";

    /*作业下载文件的url前缀*/
    public static final String HOMEWORK_FILE_DOWNLOAD_URL_PREFIX = "homework_file_download_url_prefix:";

    /*作业下载分布式锁的前缀*/
    public static final String HOMEWORK_FILE_DOWNLOAD_LOCK_PREFIX = "homework_file_download_lock_prefix:";

    /*用户班级角色表缓存前缀*/
    public static final String USER_CLASS_ROLE_PREFIX = "user_class_role_prefix:";

    /*用户班级角色表的锁前缀*/
    public static final String USER_CLASS_ROLE_LOCK_PREFIX = "user_class_role_lock_prefix:";

    /*班级信息缓存的前缀*/
    public static final String CLASS_INFO_PREFIX = "class_info_prefix:";

    /*班级信息缓存锁的前缀*/
    public static final String CLASS_INFO_LOCK_PREFIX = "class_info_lock_prefix:";

    /*作业信息缓存的前缀*/
    public static final String HOMEWORK_INFO_PREFIX = "homework_info_prefix:";

    /*作业信息缓存锁的前缀*/
    public static final String HOMEWORK_INFO_LOCK_PREFIX = "homework_info_lock_prefix:";
}
