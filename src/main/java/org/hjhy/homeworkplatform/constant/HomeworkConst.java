package org.hjhy.homeworkplatform.constant;

/**
 * @author HJHY
 * @date 2024/2/27 17:19
 */
public class HomeworkConst {
    /**
     * 作业提交凭证的有效实践为5分钟(300000ms)
     */
    public static long HOMEWORK_SUBMISSION_TOKEN_EXPIRE_TIME = 300000;

    /**
     * 作业下载token过期时间为5分钟(300000ms)
     */
    public static long HOMEWORK_DOWNLOAD_TOKEN_EXPIRE_TIME = 300000;

    /**
     * 作业信息缓存的时间为1小时
     */
    public static long HOMEWORK_INFO_CACHE_TIME = 1;
}
