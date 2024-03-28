package org.hjhy.homeworkplatform.generator.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * 推送设置表
 *
 * @TableName push_setting
 */
@TableName(value = "push_setting")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class PushSetting implements Serializable {
    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 提前推送的时间(单位:h)
     */
    private Integer advanceTime;
}