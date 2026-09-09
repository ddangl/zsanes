package com.anes.schedule.common;

import lombok.Getter;

/** 错误码(与 HTTP 语义对齐,业务错误以 body.code 返回) */
@Getter
public enum ErrorCode {

    OK(0, "成功"),
    BAD_REQUEST(400, "请求参数或业务校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常,请联系管理员");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
