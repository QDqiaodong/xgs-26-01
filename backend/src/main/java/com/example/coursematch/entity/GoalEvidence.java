package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 目标证明材料与督导补充/退回结论。 */
@Entity
@Table(name = "goal_evidence", uniqueConstraints =
        @UniqueConstraint(name = "uk_evidence_idempotency", columnNames = {"goal_id", "client_token"}))
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

    @Column(name = "client_token", nullable = false, length = 80)
    private String clientToken;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "material_uri", length = 500)
    private String materialUri;

    @Column(name = "submitted_by", nullable = false, length = 50)
    private String submittedBy;

    @Column(name = "submitted_by_name", length = 50)
    private String submittedByName;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    /** SUBMITTED / SUPPLEMENT_REQUESTED / REJECTED / APPROVED */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "SUBMITTED";

    @Column(name = "review_comment", length = 2000)
    private String reviewComment;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_by_name", length = 50)
    private String reviewedByName;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (status == null) status = "SUBMITTED";
    }
}
