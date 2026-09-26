package com.example.coursematch.controller;

import com.example.coursematch.entity.Role;
import com.example.coursematch.entity.User;
import com.example.coursematch.repository.RoleRepository;
import com.example.coursematch.repository.UserRepository;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.CurrentUserProvider;
import com.example.coursematch.security.TokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色边界在服务端强制执行：查看者只读，系统管理员不能代替专业评审/发布。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchPlanAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TokenStore tokenStore;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String viewerToken;
    private String adminToken;
    private String instructorAdminToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        ensureRole("ADMIN");
        ensureRole("COURSE_ADMIN");
        ensureRole("INSTRUCTOR_ADMIN");
        ensureRole("VIEWER");
        viewerToken = token("viewer_user", "VIEWER");
        adminToken = token("sys_admin_user", "ADMIN");
        instructorAdminToken = token("instructor_admin_user", "INSTRUCTOR_ADMIN");
    }

    private void ensureRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder().name(name).description(name).active(true).build());
        }
    }

    private String token(String username, String role) {
        // 幂等：已存在则直接签发新 token
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            Role r = roleRepository.findByName(role).orElseThrow();
            return userRepository.save(User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("x"))
                    .realName(username)
                    .roles(Set.of(r))
                    .enabled(true)
                    .build());
        });
        userId = user.getId();
        return tokenStore.create(AuthUser.builder()
                .userId(user.getId())
                .username(username)
                .realName(username)
                .roles(Set.of(role))
                .build());
    }

    @AfterEach
    void tearDown() {
        CurrentUserProvider.clear();
    }

    @Test
    void anonymousCannotAccessPlans() throws Exception {
        mockMvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCanReadButCannotSubmitReviewOrPublish() throws Exception {
        mockMvc.perform(get("/api/plans").header("Authorization", viewerToken))
                .andExpect(status().isOk());

        // 查看者不能提交评审意见
        mockMvc.perform(post("/api/plans/999/reviews/course")
                        .header("Authorization", viewerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("decision", "APPROVED", "comment", "x"))))
                .andExpect(status().isForbidden());

        // 查看者不能发布
        mockMvc.perform(post("/api/plans/999/publish").header("Authorization", viewerToken))
                .andExpect(status().isForbidden());

        // 查看者不能创建草稿
        mockMvc.perform(post("/api/plans").header("Authorization", viewerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("matchRecordId", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdminCanRejectButCannotPublishOrDoProfessionalReview() throws Exception {
        // ADMIN 不能代替课程管理员的专业评审
        mockMvc.perform(post("/api/plans/999/reviews/course")
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("decision", "APPROVED", "comment", "x"))))
                .andExpect(status().isForbidden());

        // ADMIN 不能代替讲师管理员的专业评审
        mockMvc.perform(post("/api/plans/999/reviews/instructor")
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("decision", "APPROVED", "comment", "x"))))
                .andExpect(status().isForbidden());

        // ADMIN 不能发布
        mockMvc.perform(post("/api/plans/999/publish").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        // ADMIN 不能正式指派
        mockMvc.perform(post("/api/plans/999/assign").header("Authorization", adminToken))
                .andExpect(status().isForbidden());

        // ADMIN 的驳回入口允许访问（404/400 而非 403，说明通过了角色校验）
        mockMvc.perform(post("/api/plans/999/reject")
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("reason", "测试驳回"))))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s == 403) {
                        throw new AssertionError("系统管理员应当能访问驳回接口，但得到 403");
                    }
                });
    }

    @Test
    void instructorAdminCannotPublishOrCreateDraft() throws Exception {
        mockMvc.perform(post("/api/plans").header("Authorization", instructorAdminToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("matchRecordId", 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/plans/999/publish").header("Authorization", instructorAdminToken))
                .andExpect(status().isForbidden());
    }
}
