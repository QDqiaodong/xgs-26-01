package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 推演方案（带版本）。
 *
 * <p>方案在草稿创建时即固化快照：课程要求、权重配置、候选讲师关键资料与五项评分明细、
 * 候选排序均冗余在本方案及其子表中，之后查看/评审/发布都以快照为准，不按最新资料重算。
 *
 * <p>状态机：
 * DRAFT（草稿，可改候选/可删）
 *  -> IN_REVIEW（已提交评审，候选锁定）
 *  -> APPROVED（两类专业评审均通过，可发布）
 *  -> PUBLISHED（已发布；同课程同时刻仅一个有效发布版本）
 *  -> SUPERSEDED（被更新发布版本替代，仍可追溯，但不得再发起正式指派）
 * REJECTED（评审驳回，可修改后重新提交，重新提交时旧评审意见清空）
 * INVALIDATED（草稿创建后依赖资料变化导致失效，终态）
 */
@Entity
@Table(name = "match_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_match_plans_course_version",
                        columnNames = {"course_id", "version_no"})
        },
        indexes = {
                @Index(name = "idx_match_plans_course_status", columnList = "course_id,status"),
                @Index(name = "idx_match_plans_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 来源匹配推演记录 */
    @Column(name = "match_record_id", nullable = false)
    private Long matchRecordId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    /** 同课程内自增的方案版本号，从 1 开始 */
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 100)
    private String name;

    /** DRAFT / IN_REVIEW / APPROVED / REJECTED / PUBLISHED / SUPERSEDED / INVALIDATED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    // ---------------- 课程要求快照 ----------------
    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "course_industry", length = 50)
    private String courseIndustry;

    @Column(name = "course_difficulty", length = 20)
    private String courseDifficulty;

    @Column(name = "course_required_qualifications", length = 500)
    private String courseRequiredQualifications;

    @Column(name = "course_style_requirements", length = 200)
    private String courseStyleRequirements;

    @Column(name = "course_status", length = 20)
    private String courseStatus;

    // ---------------- 权重配置快照 ----------------
    @Column(name = "weight_config_id")
    private Long weightConfigId;

    @Column(name = "weight_config_name", length = 100)
    private String weightConfigName;

    @Column(name = "teaching_years_weight")
    private Double teachingYearsWeight;

    @Column(name = "qualification_weight")
    private Double qualificationWeight;

    @Column(name = "rating_weight")
    private Double ratingWeight;

    @Column(name = "expertise_weight")
    private Double expertiseWeight;

    @Column(name = "style_weight")
    private Double styleWeight;

    @Column(name = "gap_threshold")
    private Double gapThreshold;

    // ---------------- 快照汇总 ----------------
    @Column(name = "best_score")
    private Double bestScore;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    /** 正式指派采用的候选（讲师ID），仅 PUBLISHED 后指派成功时写入 */
    @Column(name = "selected_instructor_id")
    private Long selectedInstructorId;

    @Column(name = "selected_instructor_name", length = 50)
    private String selectedInstructorName;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // ---------------- 失效信息 ----------------
    /** 失效变化来源分类：COURSE / INSTRUCTOR / SKILL / WEIGHT / CAPACITY */
    @Column(name = "invalid_source_type", length = 20)
    private String invalidSourceType;

    /** 失效来源对象 ID（如变化的讲师、权重配置 ID） */
    @Column(name = "invalid_source_id")
    private Long invalidSourceId;

    @Column(name = "invalid_reason", length = 500)
    private String invalidReason;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    // ---------------- 审计字段 ----------------
    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "submitted_by", length = 50)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_by", length = 50)
    private String publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "superseded_by_plan_id")
    private Long supersededByPlanId;

    @Column(length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "DRAFT";
        }
        if (createdBy == null) {
            createdBy = "system";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
