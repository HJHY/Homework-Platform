package org.hjhy.homeworkplatform.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author HJHY
 * @date 2023/1/12 14:45
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HomeworkReleaseDto {
    /**
     * 班级id
     */
    private Integer classId;

    /**
     * 作业名
     */
    @NotBlank(message = "作业名称不能为空")
    private String homeworkName;
    /**
     * 截止时间
     */
    @NotNull(message = "截至日期不能为空")
    @Future(message = "结束的时间应该在未来")
    private Date endTime;
    /**
     * 作业描述/备注
     */
    private String description;
}
