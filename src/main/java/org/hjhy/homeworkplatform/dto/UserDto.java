package org.hjhy.homeworkplatform.dto;

import lombok.*;


/**
 * @author HJHY
 * @date 2023/1/8 15:40
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class UserDto {
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
     * 学生id(学生学号)
     */
    private String studentId;
    /**
     * 班级id
     */
    private Integer classId;
}
