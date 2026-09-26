package com.example.coursematch.security;

/** 已认证但无权限（403） */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
