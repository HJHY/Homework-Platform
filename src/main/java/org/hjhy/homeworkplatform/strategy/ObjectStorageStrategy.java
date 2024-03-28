package org.hjhy.homeworkplatform.strategy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.vo.FileDownloadVo;
import org.hjhy.homeworkplatform.vo.FileUploadVo;

/**
 * @author HJHY
 * @date 2024/2/29 21:51
 */
public interface ObjectStorageStrategy {
    /**
     * 上传作业文件
     *
     * @param filePath 文件路径
     * @return {@link String} 服务器签名
     */
    FileUploadVo uploadHomework(String filePath, Integer homeworkId) throws Exception;

    /**
     * 下载作业文件
     * @param sourceDir 原始文件存储路径
     * @param filePath 文件路径
     * @return {@link FileDownloadVo}
     * @throws Exception exception
     */
    FileDownloadVo downloadHomework(String sourceDir, String filePath) throws Exception;

    /**
     * 判断文件是否存在
     * @param filePath 文件路径
     * @return {@link Boolean}
     */
    boolean exists(String filePath);

    /**
     * 删除作业文件
     * @param filePath 文件路径
     */
    void deleteHomework(String filePath);

    boolean verifyOSSCallbackRequest(FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request) throws Exception;

    void responseUploadCallback(HttpServletRequest request, HttpServletResponse response, boolean res) throws Exception;
}
