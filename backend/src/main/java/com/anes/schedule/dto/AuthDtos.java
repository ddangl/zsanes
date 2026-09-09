package com.anes.schedule.dto;

import jakarta.validation.constraints.NotBlank;

/** 认证相关请求/响应对象 */
public class AuthDtos {

    public record LoginReq(@NotBlank(message = "用户名不能为空") String username,
                           @NotBlank(message = "密码不能为空") String password) {
    }

    public record LoginResp(String token, UserInfo user) {
    }

    public record UserInfo(String username, String name, String role) {
    }
}
