package org.hjhy.homeworkplatform.dto;

import lombok.*;

/**
 * @author HJHY
 * @date 2024/3/29 14:50
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class ClazzConditionDto {
    /**
     * 班级名称
     */
    private String className;
    /**
     * 班级描述
     */
    private String description;
    /**
     * 创建者id
     */
    private Integer creatorId;
    /**
     * 是否有效
     */
    private Integer isValid;
}
