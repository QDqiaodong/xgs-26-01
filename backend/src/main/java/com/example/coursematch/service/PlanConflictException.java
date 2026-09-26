package com.example.coursematch.service;

/**
 * 方案链路并发冲突（409）：重复发布、重复评审、重复指派、状态已被并发请求改变等。
 * 消息应明确告知请求方最终结果，便于超时重试后分辨“自己成功了”还是“别人已完成”。
 */
public class PlanConflictException extends RuntimeException {
    public PlanConflictException(String message) {
        super(message);
    }
}
