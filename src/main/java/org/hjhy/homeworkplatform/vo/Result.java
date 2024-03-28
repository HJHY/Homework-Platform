package org.hjhy.homeworkplatform.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hjhy.homeworkplatform.constant.StatusCode;

/**
 * 接口返回类
 *
 * @author yezhiqiu
 * @date 2021/08/10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 返回码
     */
    private Integer code;
    /**
     * 返回信息
     */
    private String message;
    /**
     * 返回数据
     */
    private T data;

    public static <T> Result<T> ok() {
        return restResult(null, StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> ok(T data) {
        return restResult(data, StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> ok(T data, String message) {
        return restResult(data, StatusCode.SUCCESS.getCode(), message);
    }

    public static <T> Result<T> fail() {
        return restResult(null, StatusCode.FAIL.getCode(), StatusCode.FAIL.getMessage());
    }

    public static <T> Result<T> fail(String message) {
        return restResult(null, StatusCode.FAIL.getCode(), message);
    }

    public static <T> Result<T> fail(T data) {
        return restResult(data, StatusCode.FAIL.getCode(), StatusCode.FAIL.getMessage());
    }

    public static <T> Result<T> fail(T data, String message) {
        return restResult(data, StatusCode.FAIL.getCode(), message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return restResult(null, code, message);
    }

    private static <T> Result<T> restResult(T data, Integer code, String message) {
        Result<T> apiResult = new Result<>();
        apiResult.setData(data);
        apiResult.setCode(code);
        apiResult.setMessage(message);
        return apiResult;
    }

}
