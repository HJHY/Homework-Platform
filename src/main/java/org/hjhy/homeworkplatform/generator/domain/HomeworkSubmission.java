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
 * 作业提交表
 *
 * @TableName homework_submission
 */
@TableName(value = "homework_submission")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class HomeworkSubmission implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 提交的作业id
     */
    @TableId(type = IdType.AUTO)
    private Integer homeworkId;
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 所属发布作业的id
     */
    private Integer belongingId;
    /**
     * 作业描述
     */
    private String description;
    /**
     * 文件路径
     */
    private String fileSrc;
    /**
     * 状态0:签名已生成但是还未提交;状态1.已成功提交
     */
    private Integer status;
    /**
     * 是否有效
     */
    private Integer isValid;
    /**
     * 创建时间
     */
    private Date createTime;
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
        HomeworkSubmission other = (HomeworkSubmission) that;
        return (this.getHomeworkId() == null ? other.getHomeworkId() == null : this.getHomeworkId().equals(other.getHomeworkId()))
                && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
                && (this.getBelongingId() == null ? other.getBelongingId() == null : this.getBelongingId().equals(other.getBelongingId()))
                && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
                && (this.getFileSrc() == null ? other.getFileSrc() == null : this.getFileSrc().equals(other.getFileSrc()))
                && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
                && (this.getIsValid() == null ? other.getIsValid() == null : this.getIsValid().equals(other.getIsValid()))
                && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
                && (this.getLastUpdateTime() == null ? other.getLastUpdateTime() == null : this.getLastUpdateTime().equals(other.getLastUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getHomeworkId() == null) ? 0 : getHomeworkId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getBelongingId() == null) ? 0 : getBelongingId().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getFileSrc() == null) ? 0 : getFileSrc().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getIsValid() == null) ? 0 : getIsValid().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getLastUpdateTime() == null) ? 0 : getLastUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [" +
                "Hash = " + hashCode() +
                ", homeworkId=" + homeworkId +
                ", userId=" + userId +
                ", belongingId=" + belongingId +
                ", description=" + description +
                ", fileSrc=" + fileSrc +
                ", status=" + status +
                ", isValid=" + isValid +
                ", createTime=" + createTime +
                ", lastUpdateTime=" + lastUpdateTime +
                ", serialVersionUID=" + serialVersionUID +
                "]";
    }
}