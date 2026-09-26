package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 方案评审意见。
 *
 * <p>两类专业评审：
 * <ul>
 *   <li>INSTRUCTOR：讲师管理员核对讲师资质与可授课状态</li>
 *   <li>COURSE：课程管理员核对课程要求与采用的权重</li>
 * </ul>
 *
 * <p>唯一约束 (plan_id, review_type) 保证：重复点击、超时重试、两名管理员同时提交
 * 都不会产生重复评审记录。系统管理员（ADMIN）走独立的驳回动作，不写本表，
 * 因此不能替代任何一类缺失的专业评审。
 */
@Entity
@Table(name = "plan_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_reviews_plan_type",
                        columnNames = {"plan_id", "review_type"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** INSTRUCTOR（讲师管理员意见） / COURSE（课程管理员意见） */
    @Column(name = "review_type", nullable = false, length = 20)
    private String reviewType;

    /** APPROVED / REJECTED */
    @Column(nullable = false, length = 20)
    private String decision;

    @Column(length = 500)
    private String comment;

    @Column(name = "reviewer_username", length = 50)
    private String reviewerUsername;

    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
