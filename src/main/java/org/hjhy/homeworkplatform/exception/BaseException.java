package org.hjhy.homeworkplatform.exception;

import lombok.Getter;
import org.hjhy.homeworkplatform.constant.StatusCode;

/**
 * @author HJHY
 * @date 2023/1/6 23:52
 */
@Getter
public class BaseException extends RuntimeException {
    /**
     * 异常代码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    public BaseException(String message) {
        this.code = StatusCode.FAIL.getCode();
        this.message = message;
    }

    public BaseException(StatusCode statusCode) {
        this.code = statusCode.getCode();
        this.message = statusCode.getMessage();
    }

}
