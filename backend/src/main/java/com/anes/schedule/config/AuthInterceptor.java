package com.anes.schedule.config;

import com.anes.schedule.common.AuthContext;
import com.anes.schedule.common.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器:白名单外的请求校验 Authorization: Bearer <token>,
 * 通过后把当前用户写入 AuthContext(ThreadLocal),请求结束清理。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            Claims claims = jwtUtil.parse(auth.substring(7));
            if (claims != null) {
                Long userId = Long.valueOf(claims.getSubject());
                AuthContext.set(new AuthContext.CurrentUser(
                        userId,
                        (String) claims.get("username"),
                        (String) claims.get("role"),
                        null));
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }
}
