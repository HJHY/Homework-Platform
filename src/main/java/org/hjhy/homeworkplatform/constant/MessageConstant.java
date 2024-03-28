package org.hjhy.homeworkplatform.constant;

/**
 * @author HJHY
 * @date 2024/2/29 19:05
 */
public class MessageConstant {
    public static final String HOMEWORK_RELEASE_MESSAGE = """
            亲爱的%s,
                您好!你所在的班级 <%s> 发布了新作业 <%s> ,截止时间为 %s ,作业备注: <%s> ,请注意查收!""";

    public static final String DDL_MESSAGE = """
            亲爱的%s,
                您好!你所在的班级 <%s> 的新作业 <%s> 尚未提交,截止时间为 %s ,作业备注: <%s> ,请注意及时提交!""";

    public static String HOMEWORK_REMINDER_MESSAGE = """
            亲爱的%s,
                您好!你所在的班级 <%s> 的作业 <%s> 还未进行提交,截止时间为 %s ,作业备注: <%s> ,请注意按时提交!
                如已提交请忽略本提醒信息!""";
}
