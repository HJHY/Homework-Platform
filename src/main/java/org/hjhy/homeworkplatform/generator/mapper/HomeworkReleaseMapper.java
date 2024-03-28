package org.hjhy.homeworkplatform.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.springframework.stereotype.Repository;

/**
 * @author 13746
 * @description 针对表【homework_release(作业发布表)】的数据库操作Mapper
 * @createDate 2024-02-19 20:47:51
 * @Entity generator.domain.HomeworkRelease
 */
@Repository
public interface HomeworkReleaseMapper extends BaseMapper<HomeworkRelease> {
    IPage<HomeworkRelease> getUnSubmittedHomework(Integer userId, Page<HomeworkRelease> page, Integer roleId);
}




