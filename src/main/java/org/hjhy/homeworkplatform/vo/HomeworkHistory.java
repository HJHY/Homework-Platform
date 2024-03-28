package org.hjhy.homeworkplatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hjhy.homeworkplatform.generator.domain.HomeworkSubmission;

import java.util.Date;

/**
 * @author HJHY
 * @date 2023/1/16 15:23
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class HomeworkHistory {
    /**
     * 提交的作业id
     */
    private Integer homeworkSubmitId;
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 所属发布作业的id
     */
    private Integer homeworkBelongId;
    /**
     * 作业描述/备注
     */
    private String description;

    /**
     * 作业状态
     */
    private Integer status;
    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date submitTime;

    public HomeworkHistory(HomeworkSubmission homeworkSubmission) {
        this.homeworkSubmitId = homeworkSubmission.getHomeworkId();
        this.userId = homeworkSubmission.getUserId();
        this.homeworkBelongId = homeworkSubmission.getBelongingId();
        this.description = homeworkSubmission.getDescription();
        this.submitTime = homeworkSubmission.getCreateTime();
        this.status = homeworkSubmission.getStatus();
    }
}
