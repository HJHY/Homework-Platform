package org.hjhy.homeworkplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * @author HJHY
 * @date 2024/3/29 15:08
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class HomeworkReleaseConditionDto {
    /**
     * 发布的作业id
     */
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
     * 创建时间
     */
    private Date launchTime;
    /**
     * 截止时间
     */
    private Date endTime;
    /**
     * 是否有效
     */
    private Integer isValid = 1;
    /**
     * 作业描述/备注
     */
    private String description;
}
