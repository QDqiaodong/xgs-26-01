package com.example.coursematch.security;

/** 未认证（401） */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
