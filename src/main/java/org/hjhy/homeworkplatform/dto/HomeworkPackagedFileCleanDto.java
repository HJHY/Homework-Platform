package org.hjhy.homeworkplatform.dto;

import lombok.*;

/**
 * @author HJHY
 * @date 2024/2/28 15:53
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class HomeworkPackagedFileCleanDto {
    String bucketName;
    String filePath;
}
