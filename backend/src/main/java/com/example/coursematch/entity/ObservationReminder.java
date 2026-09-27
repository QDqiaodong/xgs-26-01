package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待处理提醒。每种待处理状态都产生带明确类型、责任人的提醒，不合并成笼统“处理中”。
 *
 * <p>去重：同一 observation_id + reminder_type + dedup_key（如目标ID/证据ID/状态轮次）
 * 仅保留一条 OPEN 提醒（数据库唯一约束兜底）；状态被处理后提醒置 RESOLVED，
 * 再次进入同类状态用新的 dedup_key 产生新提醒。超时扫描、重复点击、网络重试都不会重复提醒。
 */
@Entity
@Table(name = "observation_reminders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_obs_reminder_dedup",
                columnNames = {"observation_id", "reminder_type", "dedup_key"}),
        indexes = {
                @Index(name = "idx_obs_reminder_assignee", columnList = "assignee_username,status"),
                @Index(name = "idx_obs_reminder_obs", columnList = "observation_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** 提醒类型，见服务层 ObservationReminderTypes，例如 ACK_DUE / DISPUTE_REVIEW / EVIDENCE_REVIEW /
     *  REVIEW_OVERDUE / OWNER_CLOSE / REOPEN_APPROVAL */
    @Column(name = "reminder_type", nullable = false, length = 30)
    private String reminderType;

    /** 去重键：状态进入次数、目标/证据ID等，保证同一待处理事实只提醒一次 */
    @Column(name = "dedup_key", nullable = false, length = 80)
    private String dedupKey;

    /** 责任处理人用户名（讲师/督导/负责人）；广播类为 ROLE:XXX */
    @Column(name = "assignee_username", length = 50)
    private String assigneeUsername;

    @Column(name = "assignee_name", length = 50)
    private String assigneeName;

    @Lob
    @Column(name = "message", nullable = false)
    private String message;

    /** OPEN 待处理 / RESOLVED 已处理 */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "OPEN";

    /** 截止时间（逾期类提醒使用） */
    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_note", length = 300)
    private String resolvedNote;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "OPEN";
        }
    }
}
