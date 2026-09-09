package com.anes.schedule.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** 全局异常兜底:所有异常统一转为 ApiResponse,不向前端泄露堆栈 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常:预期内,按错误码返回,不打堆栈 */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    /** 参数校验失败:400 + 字段级错误明细 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(ErrorCode.BAD_REQUEST, detail.isEmpty() ? "参数校验失败" : detail);
    }

    /** 兜底:500,完整堆栈只进日志 */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getDefaultMessage());
    }
}
