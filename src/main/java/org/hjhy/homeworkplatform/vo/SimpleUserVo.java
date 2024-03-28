package org.hjhy.homeworkplatform.vo;

import lombok.*;

/**
 * @author HJHY
 * @date 2024/2/7 17:11
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class SimpleUserVo {
    /**
     * 用户id
     */
    private Integer id;
    /**
     * 用户名
     */
    private String username;
    /**
     * 真实姓名
     */
    private String realname;
    /**
     * 性别
     */
    private String sex;
    /**
     * 头像路径
     */
    private String iconUrl;
    /**
     * 学校id
     */
    private String schoolId;
    /**
     * 学生id(学生学号)
     */
    private String studentId;
    /**
     * 学校班级id(指学生所在学校的班级id)
     */
    private Integer classId;
}
