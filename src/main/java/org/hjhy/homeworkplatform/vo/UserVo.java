package org.hjhy.homeworkplatform.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hjhy.homeworkplatform.generator.domain.User;

import java.util.Date;

/**
 * @author HJHY
 * @date 2024/2/7 17:13
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class UserVo {
    /**
     * 用户id
     */
    @TableId(type = IdType.AUTO)
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
     * 电子邮箱
     */
    private String email;
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
    /**
     * 是否禁用
     */
    private Integer isDisable;
    /**
     * 是否有效
     */
    private Integer isValid;
    /**
     * 注册时间
     */
    private Date registerTime;
    /**
     * 最后修改时间
     */
    private Date lastUpdateTime;

    public UserVo(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.realname = user.getRealname();
        this.email = user.getEmail();
        this.sex = user.getSex();
        this.iconUrl = user.getIconUrl();
        this.studentId = user.getStudentId();
        this.classId = user.getClassId();
        this.registerTime = user.getRegisterTime();
        this.lastUpdateTime = user.getLastUpdateTime();
    }
}
