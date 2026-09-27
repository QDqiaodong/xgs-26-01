package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 追加式完整时间线。业务键防止网络超时重试产生重复事件。 */
@Entity
@Table(name = "observation_timeline_events", uniqueConstraints =
        @UniqueConstraint(name = "uk_obs_timeline_idempotency", columnNames = {"observation_id", "event_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "event_key", nullable = false, length = 120)
    private String eventKey;

    @Column(name = "source_type", length = 40)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(length = 2000)
    private String detail;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(name = "actor_name", length = 50)
    private String actorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
