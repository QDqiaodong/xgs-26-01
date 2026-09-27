package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 周期决策版本：结案、延长、强化辅导、重开结论均追加新版本。
 * 新版本只声明替代关系，不覆盖旧结论文本。
 */
@Entity
@Table(name = "observation_cycle_decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationCycleDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** CLOSE / EXTEND / ESCALATE / REOPEN_APPROVAL / REOPENED_CONCLUSION */
    @Column(nullable = false, length = 30)
    private String decisionType;

    @Column(nullable = false, length = 2000)
    private String conclusion;

    @Column(name = "basis", nullable = false, length = 2000)
    private String basis;

    @Column(name = "decided_by", nullable = false, length = 50)
    private String decidedBy;

    @Column(name = "decided_by_name", nullable = false, length = 50)
    private String decidedByName;

    @Column(name = "replaces_decision_id")
    private Long replacesDecisionId;

    /** ACTIVE / SUPERSEDED / HISTORICAL */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "client_token", length = 80)
    private String clientToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
