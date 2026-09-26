package com.example.coursematch.service;

import com.example.coursematch.dto.request.LoginRequest;
import com.example.coursematch.dto.request.UserCreateRequest;
import com.example.coursematch.dto.response.LoginResponse;
import com.example.coursematch.entity.User;

import java.util.List;

public interface UserService {
    
    LoginResponse login(LoginRequest request);
    
    void logout(String token);
    
    User createUser(UserCreateRequest request);
    
    User getUserById(Long id);
    
    User getUserByUsername(String username);
    
    List<User> getAllUsers();
    
    User updateUser(Long id, UserCreateRequest request);
    
    void deleteUser(Long id);
    
    List<User> getUserList();
}