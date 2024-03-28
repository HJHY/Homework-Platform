package org.hjhy.homeworkplatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;

import java.util.Date;

/**
 * @author HJHY
 * @date 2023/1/12 15:04
 */
@AllArgsConstructor
@Data
public class HomeworkReleaseVo {
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
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endTime;
    /**
     * 作业描述/备注
     */
    private String description;
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date launchTime;
    private String className;
    private String creatorName;

    public HomeworkReleaseVo(HomeworkRelease homeworkRelease) {
        this.homeworkId = homeworkRelease.getHomeworkId();
        this.classId = homeworkRelease.getClassId();
        this.homeworkName = homeworkRelease.getHomeworkName();
        this.creatorId = homeworkRelease.getCreatorId();
        this.endTime = homeworkRelease.getEndTime();
        this.description = homeworkRelease.getDescription();
        this.launchTime = homeworkRelease.getLaunchTime();
    }
}
