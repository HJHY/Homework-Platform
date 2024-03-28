package org.hjhy.homeworkplatform.vo;

import lombok.*;

import java.util.Date;

/**
 * @author HJHY
 * @date 2024/3/25 22:12
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class HomeworkSubmissionVo {
    private Integer submitHomeworkId;
    /**
     * 所属发布作业的id
     */
    private Integer homeworkId;

    /**
     * 最新提交时间
     */
    private Date lastSubmitTime;

    /**
     * 作业名称
     */
    private String homeworkName;
}
