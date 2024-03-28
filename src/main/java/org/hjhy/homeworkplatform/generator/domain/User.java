package org.hjhy.homeworkplatform.generator.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 *
 * @TableName user
 */
@TableName(value = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
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
     * 密码
     */
    private String password;
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

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        User other = (User) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId())) && (this.getUsername() == null ? other.getUsername() == null : this.getUsername().equals(other.getUsername())) && (this.getRealname() == null ? other.getRealname() == null : this.getRealname().equals(other.getRealname())) && (this.getPassword() == null ? other.getPassword() == null : this.getPassword().equals(other.getPassword())) && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail())) && (this.getSex() == null ? other.getSex() == null : this.getSex().equals(other.getSex())) && (this.getIconUrl() == null ? other.getIconUrl() == null : this.getIconUrl().equals(other.getIconUrl())) && (this.getSchoolId() == null ? other.getSchoolId() == null : this.getSchoolId().equals(other.getSchoolId())) && (this.getStudentId() == null ? other.getStudentId() == null : this.getStudentId().equals(other.getStudentId())) && (this.getClassId() == null ? other.getClassId() == null : this.getClassId().equals(other.getClassId())) && (this.getIsDisable() == null ? other.getIsDisable() == null : this.getIsDisable().equals(other.getIsDisable())) && (this.getIsValid() == null ? other.getIsValid() == null : this.getIsValid().equals(other.getIsValid())) && (this.getRegisterTime() == null ? other.getRegisterTime() == null : this.getRegisterTime().equals(other.getRegisterTime())) && (this.getLastUpdateTime() == null ? other.getLastUpdateTime() == null : this.getLastUpdateTime().equals(other.getLastUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
        result = prime * result + ((getRealname() == null) ? 0 : getRealname().hashCode());
        result = prime * result + ((getPassword() == null) ? 0 : getPassword().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getSex() == null) ? 0 : getSex().hashCode());
        result = prime * result + ((getIconUrl() == null) ? 0 : getIconUrl().hashCode());
        result = prime * result + ((getSchoolId() == null) ? 0 : getSchoolId().hashCode());
        result = prime * result + ((getStudentId() == null) ? 0 : getStudentId().hashCode());
        result = prime * result + ((getClassId() == null) ? 0 : getClassId().hashCode());
        result = prime * result + ((getIsDisable() == null) ? 0 : getIsDisable().hashCode());
        result = prime * result + ((getIsValid() == null) ? 0 : getIsValid().hashCode());
        result = prime * result + ((getRegisterTime() == null) ? 0 : getRegisterTime().hashCode());
        result = prime * result + ((getLastUpdateTime() == null) ? 0 : getLastUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", id=" + id +
                ", username=" + username +
                ", realname=" + realname +
                ", password=" + password +
                ", email=" + email +
                ", sex=" + sex +
                ", iconUrl=" + iconUrl +
                ", schoolId=" + schoolId +
                ", studentId=" + studentId +
                ", classId=" + classId +
                ", isDisable=" + isDisable +
                ", isValid=" + isValid +
                ", registerTime=" + registerTime +
                ", lastUpdateTime=" + lastUpdateTime +
                ", serialVersionUID=" + serialVersionUID +
                "]";
    }
}