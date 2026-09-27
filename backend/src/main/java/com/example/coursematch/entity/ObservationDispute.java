package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 讲师对观察事实的异议。讲师只能在确认前（PENDING_ACK）提出；
 * 由另一名教学督导复核，原观察人不能自行关闭针对自己的异议。
 *
 * <p>复核可以维持（UPHELD 原项不变）、修正（MODIFIED 具体观察项，原值进修订留痕）、
 * 撤销（REVOKED 具体观察项）。复核完成后观察单回到待确认，由讲师基于当前有效项重新确认。
 * 同一名讲师在同一轮待确认期间只能提出一次异议（重复提交返回 409，不产生第二条）。
 */
@Entity
@Table(name = "observation_disputes",
        indexes = {
                @Index(name = "idx_obs_dispute_obs", columnList = "observation_id"),
                @Index(name = "idx_obs_dispute_status", columnList = "status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** 异议轮次（从 1 开始） */
    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    /** 异议涉及的观察项（可多选，逗号分隔的 item id）；也可对整体描述异议 */
    @Column(name = "item_ids", length = 300)
    private String itemIds;

    @Lob
    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "raised_by", nullable = false, length = 50)
    private String raisedBy;

    @Column(name = "raised_by_name", length = 50)
    private String raisedByName;

    @Column(name = "raised_at", nullable = false, updatable = false)
    private LocalDateTime raisedAt;

    /** OPEN 待复核 / RESOLVED 已出复核结论 / CANCELLED 讲师撤回 */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "OPEN";

    // ---------------- 复核结论 ----------------
    /** UPHELD 维持 / MODIFIED 修正 / REVOKED 撤销 / MIXED 部分修正（具体以 item 修订留痕为准） */
    @Column(name = "resolution", length = 10)
    private String resolution;

    @Lob
    @Column(name = "resolution_opinion")
    private String resolutionOpinion;

    /** 复核督导（必须不同于原观察人） */
    @Column(name = "reviewer_username", length = 50)
    private String reviewerUsername;

    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        raisedAt = LocalDateTime.now();
        if (status == null) {
            status = "OPEN";
        }
    }
}
