package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 讲师带教观察与改进周期（观察单）。
 *
 * <p>教学督导按课程场次建立，记录课堂组织、内容准确性、互动引导、时间控制四个维度的
 * 事实、等级与改进建议（明细见 {@link ObservationItem}）。观察内容分级保存：
 * 学员身份、投诉原文、内部判断等敏感内容仅督导/负责人可见，讲师只看到与本人改进相关的脱敏内容，
 * 普通查看者只看到周期状态与最终结论。
 *
 * <p>状态机（每种待处理情况都是独立状态，不合并为笼统“处理中”）：
 * <pre>
 * PENDING_ACK       待讲师确认已阅
 *  -> INSTRUCTOR_REJECTED   讲师拒绝确认（记录拒绝理由，等待督导处理）
 *  -> DISPUTED              讲师在确认前提出事实异议，等待另一名督导复核
 *  -> RESPONSE_OVERDUE      讲师逾期未确认/回应
 *  -> NO_IMPROVEMENT_NEEDED 督导确认无需改进，待负责人查看结论
 *  -> IMPROVING             讲师已确认且需要改进，分阶段目标推进中
 * IMPROVING
 *  -> EVIDENCE_PENDING_REVIEW  讲师回报证据，等待督导复查
 *  -> REVIEW_OVERDUE           督导长期未复查
 *  -> PENDING_OWNER_CLOSE      必需目标均有有效证据，等待课程负责人结案
 *  -> CLOSED                  负责人结案（结论带版本）
 *  -> EXTENDED / ESCALATED    负责人基于持续风险延长周期 / 升级强化辅导（必须写明依据）
 * CLOSED
 *  -> REOPEN_REQUESTED -> RE_REVIEW -> PENDING_OWNER_CLOSE -> CLOSED(新结论版本)
 * </pre>
 */
@Entity
@Table(name = "teaching_observations",
        indexes = {
                @Index(name = "idx_obs_course", columnList = "course_id"),
                @Index(name = "idx_obs_instructor", columnList = "instructor_id"),
                @Index(name = "idx_obs_status", columnList = "status"),
                @Index(name = "idx_obs_observer", columnList = "observer_username")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------- 关联课程场次与讲师 ----------------
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;

    /** 课程场次（同一门课的第几场授课） */
    @Column(name = "session_no", length = 50)
    private String sessionNo;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "instructor_name", length = 50)
    private String instructorName;

    /** 授课实际发生时间 */
    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    // ---------------- 观察人与附件来源 ----------------
    @Column(name = "observer_username", nullable = false, length = 50)
    private String observerUsername;

    @Column(name = "observer_name", length = 50)
    private String observerName;

    /** 附件来源说明（如：课堂录像、听课记录表、学员问卷） */
    @Column(name = "attachment_source", length = 500)
    private String attachmentSource;

    /** 观察标题/概述 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 整体观察说明（督导内部视角，按分级可见规则输出） */
    @Column(length = 2000)
    private String summary;

    // ---------------- 状态机 ----------------
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_ACK";

    /** 讲师确认已阅时间 */
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 50)
    private String acknowledgedBy;

    /** 讲师回应内容 */
    @Column(name = "instructor_response", length = 2000)
    private String instructorResponse;

    /** 讲师拒绝确认的理由（status=INSTRUCTOR_REJECTED 时） */
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    /** 讲师拒绝确认的累计次数（提醒去重轮次） */
    @Column(name = "reject_count", nullable = false)
    @Builder.Default
    private Integer rejectCount = 0;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /** 确认/回应截止时间，逾期进入 RESPONSE_OVERDUE */
    @Column(name = "ack_due_at")
    private LocalDateTime ackDueAt;

    /** 进入“待确认”状态的轮次（创建时为1，异议复核/拒绝处理后回到待确认时递增），用于确认提醒去重 */
    @Column(name = "ack_round", nullable = false)
    @Builder.Default
    private Integer ackRound = 1;

    /** 进入“改进中”状态的轮次（延长/重开复查后再进入时递增），用于进展提醒去重 */
    @Column(name = "improve_round", nullable = false)
    @Builder.Default
    private Integer improveRound = 0;

    // ---------------- 周期决策 ----------------
    /** 周期内最后一次进入改进阶段的时间（用于复查逾期计时） */
    @Column(name = "improving_since")
    private LocalDateTime improvingSince;

    /** 负责人（课程管理员）用户名 */
    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    @Column(name = "owner_name", length = 50)
    private String ownerName;

    /** 当前结案结论ID（历史版本仍在 cycle_conclusions 中保留） */
    @Column(name = "current_conclusion_id")
    private Long currentConclusionId;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** 已完成的异议轮次：每轮复核结束 +1，讲师可在下一轮待确认时再次提异议 */
    @Column(name = "dispute_rounds_resolved", nullable = false)
    @Builder.Default
    private Integer disputeRoundsResolved = 0;

    // ---------------- 重开链 ----------------
    /** 重开申请获批后，新周期结论引用的上一版结论ID（替代关系，不覆盖） */
    @Column(name = "reopened_from_conclusion_id")
    private Long reopenedFromConclusionId;

    // ---------------- 延长周期 ----------------
    @Column(name = "extend_until")
    private LocalDateTime extendUntil;

    @Column(name = "extension_count", nullable = false)
    @Builder.Default
    private Integer extensionCount = 0;

    /** 升级强化辅导的书面依据 */
    @Column(name = "escalate_reason", length = 2000)
    private String escalateReason;

    // ---------------- 审计 ----------------
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** JPA 乐观锁版本：旧页面保存过期内容时拒绝覆盖，返回 409 */
    @Version
    @Column(name = "obj_version", nullable = false)
    private Long objVersion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "PENDING_ACK";
        }
        if (disputeRoundsResolved == null) {
            disputeRoundsResolved = 0;
        }
        if (rejectCount == null) {
            rejectCount = 0;
        }
        if (extensionCount == null) {
            extensionCount = 0;
        }
        if (ackRound == null) {
            ackRound = 1;
        }
        if (improveRound == null) {
            improveRound = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
