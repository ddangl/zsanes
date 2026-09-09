package com.anes.schedule.controller;

import com.anes.schedule.common.ApiResponse;
import com.anes.schedule.common.BusinessException;
import com.anes.schedule.common.ErrorCode;
import com.anes.schedule.common.JwtUtil;
import com.anes.schedule.dto.AuthDtos;
import com.anes.schedule.entity.SysUser;
import com.anes.schedule.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Operation(summary = "登录,返回 JWT(有效期 12h)")
    @PostMapping("/login")
    public ApiResponse<AuthDtos.LoginResp> login(@RequestBody @Valid AuthDtos.LoginReq req) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.username()));
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getEnabled() == null || !user.getEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用,请联系管理员");
        }
        String token = jwtUtil.issue(user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok(new AuthDtos.LoginResp(token,
                new AuthDtos.UserInfo(user.getUsername(), user.getUsername(), user.getRole())));
    }
}
