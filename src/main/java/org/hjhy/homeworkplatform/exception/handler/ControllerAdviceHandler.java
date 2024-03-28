package org.hjhy.homeworkplatform.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.hjhy.homeworkplatform.constant.StatusCode;
import org.hjhy.homeworkplatform.exception.BaseException;
import org.hjhy.homeworkplatform.vo.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * @author HJHY
 * @date 2023/1/7 0:05
 */
@RestControllerAdvice
@Slf4j
public class ControllerAdviceHandler {
    /**
     * 处理请求参数格式不符合要求的异常
     * @param ex ex
     * @return res
     */
    @ExceptionHandler(value = {HandlerMethodValidationException.class})
    public Result<?> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        log.warn(ex.getMessage());
        return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), "请求参数格式不符合要求");
    }

    /**
     * 请求参数格式不符合要求
     * @param ex ex
     * @return res
     */
    @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
    public Result<?> handleHandlerMethodValidationException(MethodArgumentTypeMismatchException ex) {
        log.warn(ex.getMessage(), ex);
        return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), "请求参数格式不符合要求");
    }

    /**
     * 处理请求参数缺省异常
     *
     * @param ex ex
     * @return res
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error(ex.getMessage(), ex);
        var fieldError = ex.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), fieldError.getDefaultMessage());
        }
        return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), "参数不合法");
    }

    /**
     * 处理请求参数不合法的异常
     *
     * @param ex ex
     * @return ResultVo
     */
    @ExceptionHandler(value = {ConstraintViolationException.class, IllegalStateException.class})
    public Result<?> handleConstraintViolationException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        //分离异常原因(由于异常抛出时异常原因被拼接了一个调用的函数名,将函数名分离开)
        String cause = ex.getMessage().split(": ")[1];
        return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), "请求参数不合法:" + cause);
    }

    /**
     * 处理请求参数缺省异常
     *
     * @param ex ex
     * @return res
     */
    @ExceptionHandler(value = {UnexpectedTypeException.class})
    public Result<?> handleUnexpectedTypeException(UnexpectedTypeException ex) {
        log.error(ex.getMessage(), ex);
        return Result.fail(StatusCode.ILLEGAL_PARAMETER.getCode(), "请求参数类型或者格式不正确");
    }

    /**
     * 统一基本处理异常(没有特殊处理的都按这个)
     *
     * @param e ex
     * @return ResultVo
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(HttpServletRequest request, BaseException e) {
        log.error("请求路径为{},异常信息为{}", request.getServletPath(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 内部异常处理
     *
     * @return res
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail(StatusCode.FAIL, "内部异常请检查后台日志");
    }
}
