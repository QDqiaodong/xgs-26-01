package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 督导与讲师共同形成的分阶段改进目标。 */
@Entity
@Table(name = "observation_goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "stage_no", nullable = false)
    private Integer stageNo;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "measurement_criteria", nullable = false, length = 1000)
    private String measurementCriteria;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = true;

    /** DRAFT / PROPOSED / ACTIVE / SUBMITTED / APPROVED / REWORKED / SUPERSEDED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "supervisor_confirmed_by", length = 50)
    private String supervisorConfirmedBy;

    @Column(name = "supervisor_confirmed_at")
    private LocalDateTime supervisorConfirmedAt;

    @Column(name = "instructor_confirmed_by", length = 50)
    private String instructorConfirmedBy;

    @Column(name = "instructor_confirmed_at")
    private LocalDateTime instructorConfirmedAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_by_name", length = 50)
    private String createdByName;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = "DRAFT";
        if (required == null) required = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
