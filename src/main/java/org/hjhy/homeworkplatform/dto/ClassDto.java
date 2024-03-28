package org.hjhy.homeworkplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


/**
 * @author HJHY
 * @date 2023/1/9 22:18
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class ClassDto {
    /**
     * 班级名称
     */
    @NotBlank
    private String className;
    /**
     * 班级描述
     */
    private String description;
}
