package com.anes.schedule.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一响应:code=0 成功;400 业务/参数错误;401 未登录;403 无权限;500 系统异常。
 * 前端 axios 拦截器按 code 统一解包/报错。
 */
@Data
@AllArgsConstructor(staticName = "of")
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return of(0, "ok", data);
    }

    public static ApiResponse<Void> ok() {
        return of(0, "ok", null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return of(errorCode.getCode(), message, null);
    }
}
