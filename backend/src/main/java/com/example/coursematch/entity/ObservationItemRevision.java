package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 观察项原文、原等级、处理意见的不可变版本；复核修正时追加新版本。 */
@Entity
@Table(name = "observation_item_revisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationItemRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(nullable = false, length = 40)
    private String dimension;

    @Column(name = "sensitivity_level", nullable = false, length = 30)
    private String sensitivityLevel;

    @Column(name = "fact_text", nullable = false, length = 2000)
    private String factText;

    @Column(nullable = false, length = 30)
    private String grade;

    @Column(name = "advice_text", length = 2000)
    private String adviceText;

    /** ORIGINAL / REVIEW_UPHELD / REVIEW_CORRECTED / REVIEW_REVOKED */
    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @Column(name = "changed_by", length = 80)
    private String changedBy;

    @Column(name = "change_reason", length = 1000)
    private String changeReason;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
