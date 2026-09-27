package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察周期完整时间线事件。只追加、不可修改/删除，
 * 所有状态推进、确认、异议、复核、目标、证据、结案/延长/升级/重开均在此留痕。
 */
@Entity
@Table(name = "observation_events",
        indexes = @Index(name = "idx_obs_event_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    /** 事件类型，见服务层 ObservationEventTypes */
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @Column(name = "actor_name", length = 50)
    private String actorName;

    /** 状态迁移：从 -> 到（没有迁移时为空） */
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    /** 关联对象类型与ID（ITEM/DISPUTE/GOAL/EVIDENCE/CONCLUSION/REOPEN/ADDENDUM/ATTACHMENT） */
    @Column(name = "ref_type", length = 20)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Lob
    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
