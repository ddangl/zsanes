package com.anes.schedule.common;

/**
 * 当前登录用户上下文(ThreadLocal,由 AuthInterceptor 写入、afterCompletion 清理)。
 */
public final class AuthContext {

    public record CurrentUser(Long userId, String username, String role, Long staffId) {
        public boolean isAdmin() {
            return "ADMIN".equals(role);
        }
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 管理操作前置校验:未登录抛 401,非 ADMIN 抛 403 */
    public static CurrentUser requireAdmin() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage());
        }
        if (!user.isAdmin()) {
            throw BusinessException.forbidden("该操作仅总值班(ADMIN)可执行");
        }
        return user;
    }
}
