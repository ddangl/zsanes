package com.anes.schedule.common;

import lombok.Getter;

/** 业务异常:携带错误码,由 GlobalExceptionHandler 统一转为 ApiResponse */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(ErrorCode.FORBIDDEN, message);
    }
}
