package com.example.coursematch.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 登录会话主体。登录成功后由服务端保存，之后按请求头中的 token 解析，
 * 不再只依赖前端隐藏按钮。
 */
@Data
@Builder
@AllArgsConstructor
public class AuthUser {
    private Long userId;
    private String username;
    private String realName;
    private Long instructorId;
    private Set<String> roles;
    private LocalDateTime loginAt;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... roleNames) {
        if (roles == null) {
            return false;
        }
        for (String role : roleNames) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
