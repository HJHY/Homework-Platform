package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseConditionDto;
import org.hjhy.homeworkplatform.dto.HomeworkReleaseDto;
import org.hjhy.homeworkplatform.generator.domain.HomeworkRelease;
import org.hjhy.homeworkplatform.vo.HomeworkReleaseVo;
import org.hjhy.homeworkplatform.vo.PageResult;

/**
 * @author 13746
 * @description 针对表【homework_release(作业发布表)】的数据库操作Service
 * @createDate 2024-02-19 20:47:51
 */
public interface HomeworkReleaseService extends IService<HomeworkRelease> {

    HomeworkRelease saveHomework(Integer userId, HomeworkReleaseDto homeworkReleaseDto);

    void deleteHomework(Integer homeworkId);

    void updateHomework(Integer homeworkId, HomeworkReleaseDto homeworkReleaseDto);

    HomeworkReleaseVo findHomework(Integer homeworkId);

    PageResult<HomeworkRelease> myLaunch(Integer userId, Page<HomeworkRelease> page);

    PageResult<HomeworkRelease> getUncommittedHomework(Integer userId, Page<HomeworkRelease> page);

    PageResult<HomeworkRelease> condition(HomeworkReleaseConditionDto homeworkReleaseConditionDto, Page<HomeworkRelease> page) throws InterruptedException;

    void setHomeworkCache(HomeworkRelease homeworkRelease);

    HomeworkRelease getHomeworkFromCache(Integer homeworkId);

    void invalidateHomeworkCache(Integer homeworkId);

    HomeworkRelease getHomeworkFromDb(Integer homeworkId);

    HomeworkRelease getCacheableHomework(Integer homeworkId);

    void deleteHomeworkSync(Integer homeworkId);
}
