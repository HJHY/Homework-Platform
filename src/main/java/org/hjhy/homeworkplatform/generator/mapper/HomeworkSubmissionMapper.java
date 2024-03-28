package org.hjhy.homeworkplatform.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.hjhy.homeworkplatform.generator.domain.HomeworkSubmission;
import org.hjhy.homeworkplatform.vo.HomeworkSubmissionVo;
import org.springframework.stereotype.Repository;

/**
 * @author 13746
 * @description 针对表【homework_submission(作业提交表)】的数据库操作Mapper
 * @createDate 2024-02-19 20:47:41
 * @Entity generator.domain.HomeworkSubmission
 */
@Repository
public interface HomeworkSubmissionMapper extends BaseMapper<HomeworkSubmission> {
    IPage<HomeworkSubmissionVo> getSubmittedHomework(Integer userId, Page<HomeworkSubmission> page);
}




