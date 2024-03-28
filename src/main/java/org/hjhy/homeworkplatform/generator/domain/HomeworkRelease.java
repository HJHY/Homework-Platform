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
 * 作业发布表
 *
 * @TableName homework_release
 */
@TableName(value = "homework_release")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class HomeworkRelease implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 发布的作业id
     */
    @TableId(type = IdType.AUTO)
    private Integer homeworkId;
    /**
     * 班级id
     */
    private Integer classId;
    /**
     * 作业名
     */
    private String homeworkName;
    /**
     * 创建者
     */
    private Integer creatorId;
    /**
     * 截止时间
     */
    private Date endTime;
    /**
     * 是否有效
     */
    private Integer isValid;
    /**
     * 作业描述/备注
     */
    private String description;
    /**
     * 创建时间
     */
    private Date launchTime;
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
        HomeworkRelease other = (HomeworkRelease) that;
        return (this.getHomeworkId() == null ? other.getHomeworkId() == null : this.getHomeworkId().equals(other.getHomeworkId()))
                && (this.getClassId() == null ? other.getClassId() == null : this.getClassId().equals(other.getClassId()))
                && (this.getHomeworkName() == null ? other.getHomeworkName() == null : this.getHomeworkName().equals(other.getHomeworkName()))
                && (this.getCreatorId() == null ? other.getCreatorId() == null : this.getCreatorId().equals(other.getCreatorId()))
                && (this.getEndTime() == null ? other.getEndTime() == null : this.getEndTime().equals(other.getEndTime()))
                && (this.getIsValid() == null ? other.getIsValid() == null : this.getIsValid().equals(other.getIsValid()))
                && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
                && (this.getLaunchTime() == null ? other.getLaunchTime() == null : this.getLaunchTime().equals(other.getLaunchTime()))
                && (this.getLastUpdateTime() == null ? other.getLastUpdateTime() == null : this.getLastUpdateTime().equals(other.getLastUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getHomeworkId() == null) ? 0 : getHomeworkId().hashCode());
        result = prime * result + ((getClassId() == null) ? 0 : getClassId().hashCode());
        result = prime * result + ((getHomeworkName() == null) ? 0 : getHomeworkName().hashCode());
        result = prime * result + ((getCreatorId() == null) ? 0 : getCreatorId().hashCode());
        result = prime * result + ((getEndTime() == null) ? 0 : getEndTime().hashCode());
        result = prime * result + ((getIsValid() == null) ? 0 : getIsValid().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getLaunchTime() == null) ? 0 : getLaunchTime().hashCode());
        result = prime * result + ((getLastUpdateTime() == null) ? 0 : getLastUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", homeworkId=" + homeworkId +
                ", classId=" + classId +
                ", homeworkName=" + homeworkName +
                ", creatorId=" + creatorId +
                ", endTime=" + endTime +
                ", isValid=" + isValid +
                ", description=" + description +
                ", launchTime=" + launchTime +
                ", lastUpdateTime=" + lastUpdateTime +
                ", serialVersionUID=" + serialVersionUID +
                "]";
    }
}