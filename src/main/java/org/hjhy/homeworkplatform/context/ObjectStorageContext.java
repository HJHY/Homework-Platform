package org.hjhy.homeworkplatform.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.config.properties.ObjectStorageConfig;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.dto.FileUploadCallbackBodyDto;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.strategy.ObjectStorageStrategy;
import org.hjhy.homeworkplatform.vo.FileDownloadVo;
import org.hjhy.homeworkplatform.vo.FileUploadVo;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Map;

import static org.hjhy.homeworkplatform.enums.ObjectStorageModeEnum.getStrategy;

/**
 * @author HJHY
 * @date 2024/2/29 21:30
 */
@Service
@Slf4j
public class ObjectStorageContext {
    /**
     * 对象存储模式
     */
    private final ObjectStorageConfig objectStorageConfig;

    private final Map<String, ObjectStorageStrategy> objectStorageStrategyMap;


    public ObjectStorageContext(ObjectStorageConfig objectStorageConfig, Map<String, ObjectStorageStrategy> objectStorageStrategyMap) {
        this.objectStorageConfig = objectStorageConfig;
        this.objectStorageStrategyMap = objectStorageStrategyMap;
    }

    /**
     * 执行上传策略
     *
     * @param filePath filePath
     * @return 服务器签名
     */
    public FileUploadVo executeUploadStrategy(String filePath, Integer homeworkId) throws Exception {
        var strategy = getStrategy(objectStorageConfig.getMode());
        if (ObjectUtils.isEmpty(strategy)) {
            log.error("策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        return objectStorageStrategyMap.get(strategy).uploadHomework(filePath, homeworkId);
    }

    /**
     * 对目的文件夹进行打包并提供下载url
     * @param sourceDir 打包的文件夹
     * @param destFilePath 打包后的文件路径
     * @return FileDownloadVo
     * @throws Exception exception
     */
    public FileDownloadVo executeDownloadStrategy(String sourceDir, String destFilePath) throws Exception {
        var strategy = getStrategy(objectStorageConfig.getMode());
        if (ObjectUtils.isEmpty(strategy)) {
            log.error("策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        return objectStorageStrategyMap.get(strategy).downloadHomework(sourceDir, destFilePath);
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath filePath
     * @return true if exists
     */
    public boolean exists(String filePath) {
        var strategy = getStrategy(objectStorageConfig.getMode());
        if (ObjectUtils.isEmpty(strategy)) {
            log.error("策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        return objectStorageStrategyMap.get(strategy).exists(filePath);
    }

    public boolean verifyAndResponseUploadCallback(FileUploadCallbackBodyDto.AliCloud callbackBody, HttpServletRequest request) throws Exception {
        var strategy = getStrategy(objectStorageConfig.getMode());
        if (ObjectUtils.isEmpty(strategy)) {
            log.error("策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        return objectStorageStrategyMap.get(strategy).verifyOSSCallbackRequest(callbackBody, request);
    }

    public void responseUploadCallback(HttpServletRequest request, HttpServletResponse response, boolean res) throws Exception {
        var strategy = getStrategy(objectStorageConfig.getMode());
        if (ObjectUtils.isEmpty(strategy)) {
            log.error("策略模式配置异常");
            throw new BaseException(StatusCode.FAIL);
        }
        objectStorageStrategyMap.get(strategy).responseUploadCallback(request, response, res);
    }
}
