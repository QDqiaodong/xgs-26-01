package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分阶段改进目标。需要改进时由督导与讲师共同形成（任一方可发起，目标内容双方可见）。
 * 每项目标必须明确衡量标准、截止时间和证明材料要求。
 *
 * <p>状态：ACTIVE 进行中 / ACHIEVED 已取得有效证据 / SUPERSEDED 目标经双方调整后被替换（留痕）。
 */
@Entity
@Table(name = "improvement_goals",
        indexes = @Index(name = "idx_goal_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImprovementGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** 阶段序号：第 1 阶段、第 2 阶段…… */
    @Column(name = "stage_no", nullable = false)
    private Integer stageNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "description")
    private String description;

    /** 衡量标准（怎样算达成） */
    @Lob
    @Column(name = "criteria", nullable = false)
    private String criteria;

    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    /** 证明材料要求（讲师回报时必须对照） */
    @Lob
    @Column(name = "evidence_requirement", nullable = false)
    private String evidenceRequirement;

    /** 是否必需目标：必需目标全部有有效证据后负责人才可结案 */
    @Column(name = "required_flag", nullable = false)
    @Builder.Default
    private Boolean requiredFlag = true;

    /** ACTIVE / ACHIEVED / SUPERSEDED */
    @Column(nullable = false, length = 12)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_by_name", length = 50)
    private String createdByName;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    /** 达成时采用的有效证据ID */
    @Column(name = "accepted_evidence_id")
    private Long acceptedEvidenceId;

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
            status = "ACTIVE";
        }
        if (requiredFlag == null) {
            requiredFlag = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
