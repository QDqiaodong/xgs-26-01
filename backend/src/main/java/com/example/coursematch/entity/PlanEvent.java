package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 方案轨迹事件（评审轨迹 + 审计事实）。
 *
 * <p>只追加、不修改，完整记录：创建/编辑候选、提交评审、两类专业评审结论、
 * 系统管理员驳回、资料变更失效（含变化来源与字段明细）、发布、旧版本被替代、
 * 正式指派成功与失败原因。刷新或重新登录后轨迹保持一致。
 */
@Entity
@Table(name = "plan_events",
        indexes = {
                @Index(name = "idx_plan_events_plan", columnList = "plan_id,id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /**
     * CREATED / CANDIDATES_UPDATED / SUBMITTED / REVIEWED / REJECTED /
     * INVALIDATED / PUBLISHED / SUPERSEDED / ASSIGNMENT_SUCCEEDED / ASSIGNMENT_FAILED / DISCARDED
     */
    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(name = "actor_name", length = 50)
    private String actorName;

    /** 失效变化来源分类等结构化标记 */
    @Column(name = "source_type", length = 20)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
