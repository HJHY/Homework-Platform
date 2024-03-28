package org.hjhy.homeworkplatform.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hjhy.homeworkplatform.generator.domain.Clazz;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.generator.domain.Role;

import java.util.Date;
import java.util.List;

/**
 * @author HJHY
 * @date 2023/1/9 23:40
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ToString
public class ClassInfoVo {
    /**
     * 班级id
     */
    private Integer classId;
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
     * 创建者名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    /**
     * 最后修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date lastUpdateTime;
    /**
     * 用户id列表
     */
    private List<UserVo> userList;

    /**
     * 班级作业id列表
     */
    private List<HomeworkRelease> homeworkList;

    /**
     * 权限列表
     */
    private List<Role> roleList;

    public ClassInfoVo(Clazz clazz) {
        this.classId = clazz.getClassId();
        this.className = clazz.getClassName();
        this.description = clazz.getDescription();
        this.creatorId = clazz.getCreatorId();
        this.createTime = clazz.getCreateTime();
        this.lastUpdateTime = clazz.getLastUpdateTime();
    }

    public ClassInfoVo(Clazz clazz, List<UserVo> userList, List<HomeworkRelease> homeworkList, List<Role> roleList) {
        this.classId = clazz.getClassId();
        this.className = clazz.getClassName();
        this.description = clazz.getDescription();
        this.creatorId = clazz.getCreatorId();
        this.createTime = clazz.getCreateTime();
        this.lastUpdateTime = clazz.getLastUpdateTime();
        this.userList = userList;
        this.homeworkList = homeworkList;
        this.roleList = roleList;
    }
}
