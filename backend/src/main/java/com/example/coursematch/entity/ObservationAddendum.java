package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察事后补充说明。只允许追加，不允许修改/删除已有观察内容，
 * 时间线中与原始观察明确区分，杜绝“静默改掉原始记录”。
 */
@Entity
@Table(name = "observation_addenda",
        indexes = @Index(name = "idx_obs_addendum_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationAddendum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /** 内容分级：INTERNAL / SENSITIVE / IMPROVEMENT_RELATED / PUBLIC */
    @Column(name = "sensitivity_level", nullable = false, length = 25)
    @Builder.Default
    private String sensitivityLevel = "INTERNAL";

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_by_name", length = 50)
    private String createdByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
