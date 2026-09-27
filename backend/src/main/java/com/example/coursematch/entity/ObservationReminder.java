package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 明确区分待处理人与原因的提醒；唯一键保证扫描和人工重试不重复。 */
@Entity
@Table(name = "observation_reminders", uniqueConstraints =
        @UniqueConstraint(name = "uk_observation_reminder", columnNames = {"observation_id", "reminder_type", "dedupe_key"}))
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

    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType;

    /** INSTRUCTOR / OBSERVER / OTHER_SUPERVISOR / COURSE_OWNER */
    @Column(name = "assignee_role", nullable = false, length = 30)
    private String assigneeRole;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    /** PENDING / HANDLED / DISMISSED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "dedupe_key", nullable = false, length = 120)
    private String dedupeKey;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
