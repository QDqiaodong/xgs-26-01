package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一次课堂带教观察，同时也是后续改进周期的聚合根。
 * 原始事实与后续处理分表追加保存，业务状态用显式状态而不是笼统“处理中”。
 */
@Entity
@Table(name = "teaching_observations", indexes = {
        @Index(name = "idx_obs_course_session", columnList = "course_id,session_no"),
        @Index(name = "idx_obs_instructor_status", columnList = "instructor_id,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "assignment_id")
    private Long assignmentId;

    @Column(name = "course_name", nullable = false, length = 100)
    private String courseName;

    /** 课程场次/期次，例如 2026-09 第 2 场 */
    @Column(name = "session_no", nullable = false, length = 80)
    private String sessionNo;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "instructor_name", nullable = false, length = 50)
    private String instructorName;

    @Column(name = "observer_user_id", nullable = false)
    private Long observerUserId;

    @Column(name = "observer_username", nullable = false, length = 50)
    private String observerUsername;

    @Column(name = "observer_name", nullable = false, length = 50)
    private String observerName;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "attachment_source", length = 500)
    private String attachmentSource;

    @Column(name = "summary", length = 1000)
    private String summary;

    /**
     * PENDING_ACK / OBJECTION_PENDING_REVIEW / OBJECTION_REVIEWED /
     * RESPONDED / GOAL_SETTING / EVIDENCE_PENDING / FOLLOWUP_REVIEW /
     * AWAIT_COURSE_OWNER / CLOSED / REOPEN_PENDING / REOPEN_REVIEW /
     * INTENSIVE_COACHING / INSTRUCTOR_REFUSED / INSTRUCTOR_OVERDUE / SUPERVISOR_REVIEW_OVERDUE /
     * ESCALATION_REQUESTED
     */
    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "needs_improvement", nullable = false)
    @Builder.Default
    private Boolean needsImprovement = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "response_text", length = 2000)
    private String responseText;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "response_due_at")
    private LocalDateTime responseDueAt;

    @Column(name = "refused_at")
    private LocalDateTime refusedAt;

    @Column(name = "refuse_reason", length = 1000)
    private String refuseReason;

    @Column(name = "followup_due_at")
    private LocalDateTime followupDueAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    /** 结案后申请重开时指向被替代的历史结论版本 */
    @Column(name = "current_conclusion_id")
    private Long currentConclusionId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Version
    @Column(name = "version_lock")
    private Long versionLock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) {
            status = "PENDING_ACK";
        }
        if (needsImprovement == null) {
            needsImprovement = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
