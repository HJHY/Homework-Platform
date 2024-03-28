package org.hjhy.homeworkplatform.generator.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.generator.domain.HomeworkSubmission;
import org.hjhy.homeworkplatform.vo.*;

import java.util.List;

/**
 * @author 13746
 * @description 针对表【homework_submission(作业提交表)】的数据库操作Service
 * @createDate 2024-02-19 20:47:41
 */
public interface HomeworkSubmissionService extends IService<HomeworkSubmission> {

    FileUploadVo submit(Integer userId, Integer homeworkId, String description, String fileSuffix) throws Exception;

    void callback(FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request, HttpServletResponse response) throws Exception;

    FileDownloadVo pack(Integer homeworkId, String packFileName) throws Exception;

    List<HomeworkHistory> history(Integer userId, Integer homeworkId);

    void homeworkReminder(Integer homeworkId);

    PageResult<HomeworkSubmissionVo> getSubmittedHomework(Integer userId, Page<HomeworkSubmission> page);
}
