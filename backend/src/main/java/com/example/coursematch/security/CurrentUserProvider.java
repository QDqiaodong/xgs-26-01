package com.example.coursematch.security;

import java.util.Optional;

/**
 * 当前请求登录用户的线程上下文，由鉴权拦截器写入、请求结束时清理。
 */
public final class CurrentUserProvider {

    private static final ThreadLocal<AuthUser> HOLDER = new ThreadLocal<>();

    private CurrentUserProvider() {
    }

    public static void set(AuthUser user) {
        HOLDER.set(user);
    }

    public static Optional<AuthUser> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    /** 必须已登录，否则抛出 401 */
    public static AuthUser require() {
        AuthUser user = HOLDER.get();
        if (user == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return user;
    }

    /** 必须具备任一角色，否则抛出 403 */
    public static AuthUser requireAnyRole(String... roles) {
        AuthUser user = require();
        if (!user.hasAnyRole(roles)) {
            throw new ForbiddenException("当前角色无权执行该操作");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
