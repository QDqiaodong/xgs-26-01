package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察维度项。当前字段只反映“当前有效版本”，原文/原等级由 revision 表保留。
 */
@Entity
@Table(name = "observation_items", indexes = @Index(name = "idx_obs_item", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** ORGANIZATION / ACCURACY / INTERACTION / TIME_CONTROL / OTHER */
    @Column(nullable = false, length = 40)
    private String dimension;

    /** PUBLIC / INSTRUCTOR_RELATED / RESTRICTED */
    @Column(name = "sensitivity_level", nullable = false, length = 30)
    private String sensitivityLevel;

    @Column(name = "current_fact", nullable = false, length = 2000)
    private String currentFact;

    /** EXCELLENT / QUALIFIED / NEEDS_IMPROVEMENT / SERIOUS_ISSUE */
    @Column(nullable = false, length = 30)
    private String grade;

    @Column(name = "current_advice", length = 2000)
    private String currentAdvice;

    /** ACTIVE / CORRECTED / REVOKED；撤销后不作为后续目标依据 */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String effectiveStatus = "ACTIVE";

    @Column(name = "revision_no", nullable = false)
    @Builder.Default
    private Integer revisionNo = 1;

    @Column(name = "latest_revision_id")
    private Long latestRevisionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (effectiveStatus == null) effectiveStatus = "ACTIVE";
        if (revisionNo == null) revisionNo = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
