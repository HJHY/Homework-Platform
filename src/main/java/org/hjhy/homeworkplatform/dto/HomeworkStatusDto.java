package org.hjhy.homeworkplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author HJHY
 * @date 2024/2/26 18:04
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class HomeworkStatusDto {
    //这里指的是作业提交的id
    Integer homeworkId;

    //文件路径
    String filePath;
}
