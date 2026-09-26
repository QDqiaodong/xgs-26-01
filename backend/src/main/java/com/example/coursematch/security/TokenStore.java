package com.example.coursematch.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端登录令牌存储。单实例部署下保证刷新页面、重新登录期间状态一致；
 * 权限判定始终以服务端会话中的角色为准。
 */
@Slf4j
@Component
public class TokenStore {

    private static final long TTL_HOURS = 12;

    private record Entry(AuthUser user, LocalDateTime expiresAt) {
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public String create(AuthUser user) {
        evictExpired();
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Entry(user, LocalDateTime.now().plusHours(TTL_HOURS)));
        return token;
    }

    public Optional<AuthUser> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Entry entry = tokens.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.user());
    }

    public void remove(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }

    private void evictExpired() {
        Iterator<Map.Entry<String, Entry>> it = tokens.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (e.getValue().expiresAt().isBefore(LocalDateTime.now())) {
                it.remove();
            }
        }
    }
}
