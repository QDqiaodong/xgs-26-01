package com.example.coursematch.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 解析请求头中的登录令牌并写入当前请求上下文。
 * /api/plans/** 为方案评审发布链路，强制要求有效登录；其余接口保持向后兼容（有则解析）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        tokenStore.resolve(token).ifPresent(CurrentUserProvider::set);

        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/plans") && CurrentUserProvider.get().isEmpty()) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUserProvider.clear();
    }
}
