package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 周期重开申请。周期结案后又发现证据错误时发起，
 * 由课程负责人批准后恢复到 RE_REVIEW（待复查），原结论作为历史版本保留。
 *
 * <p>状态：PENDING 待负责人审批 / APPROVED 已批准（周期进入待复查）/ REJECTED 未批准。
 */
@Entity
@Table(name = "observation_reopen_requests",
        indexes = @Index(name = "idx_reopen_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationReopenRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** 发现的证据错误说明 */
    @Lob
    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "requested_by_name", length = 50)
    private String requestedByName;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /** PENDING / APPROVED / REJECTED */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "PENDING";

    @Lob
    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    @Column(name = "decided_by_name", length = 50)
    private String decidedByName;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
