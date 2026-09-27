package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 改进目标的证据回报。讲师可多次追加回报（不能覆盖已提交的证据）；
 * 督导复查时可以要求补充（INFO_REQUESTED）或退回不充分的证明（REJECTED）。
 *
 * <p>ACCEPTED 的证据是“有效证据”，必需目标各自拥有有效证据后负责人才能结案。
 */
@Entity
@Table(name = "goal_evidences",
        indexes = @Index(name = "idx_evidence_goal", columnList = "goal_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** SUBMITTED 已提交待复查 / ACCEPTED 有效 / REJECTED 不充分退回 / INFO_REQUESTED 要求补充 */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUBMITTED";

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /** 附件来源（文件名/链接清单，逗号分隔） */
    @Column(name = "attachment_names", length = 1000)
    private String attachmentNames;

    @Column(name = "submitted_by", nullable = false, length = 50)
    private String submittedBy;

    @Column(name = "submitted_by_name", length = 50)
    private String submittedByName;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /** 督导复查意见（退回/要求补充原因或通过说明） */
    @Lob
    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_by_name", length = 50)
    private String reviewedByName;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** 讲师针对“要求补充”的补充说明（只追加一次补充文本，不改变原回报内容） */
    @Lob
    @Column(name = "supplement")
    private String supplement;

    @Column(name = "supplemented_at")
    private LocalDateTime supplementedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (status == null) {
            status = "SUBMITTED";
        }
    }
}
