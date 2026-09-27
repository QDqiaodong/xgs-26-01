package com.example.coursematch.service;

/**
 * 带教观察链路并发/重复操作冲突（409）：重复确认、两名督导先后处理同一异议、
 * 负责人重复点击结案、网络超时重试、旧页面期望版本过期等。
 * 消息明确说明当前最终状态，便于请求方刷新后分辨结果。
 */
public class ObservationConflictException extends RuntimeException {
    public ObservationConflictException(String message) {
        super(message);
    }
}
