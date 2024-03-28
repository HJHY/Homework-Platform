package org.hjhy.homeworkplatform.vo;

import lombok.*;

import java.util.List;

/**
 * @author HJHY
 * @date 2024/2/28 16:15
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class RelationVo {
    /**
     * 创建的班级id列表
     */
    List<Integer> created;

    /**
     * 加入的班级id列表
     */
    List<Integer> joined;
}
