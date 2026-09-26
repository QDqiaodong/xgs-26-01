package com.example.coursematch.controller;

import com.example.coursematch.dto.request.LoginRequest;
import com.example.coursematch.dto.request.UserCreateRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.LoginResponse;
import com.example.coursematch.entity.User;
import com.example.coursematch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "用户认证", description = "用户登录、登出及用户管理相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录系统")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出系统")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        userService.logout(token);
        return ApiResponse.success("登出成功", null);
    }

    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "获取所有用户列表")
    public ApiResponse<List<User>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详情")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping("/users")
    @Operation(summary = "创建用户", description = "创建新用户")
    public ApiResponse<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success("创建成功", userService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public ApiResponse<User> updateUser(@PathVariable Long id, @Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.success("更新成功", userService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户", description = "删除指定用户")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success("删除成功", null);
    }
}