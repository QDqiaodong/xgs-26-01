package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程的“有效发布版本”槽位：每门课程至多一行（course_id 即主键）。
 *
 * <p>发布事务在悲观行锁内更新该槽位，从数据库层面保证：
 * 两名管理员同时通过最后一项评审、重复点击发布、超时重试，
 * 都只会有一个版本成为有效发布版本，不会留下两个有效版本。
 *
 * <p>旧版本被标记为 SUPERSEDED，仍保留原始快照与审计事实，可追溯，
 * 但不再是槽位指向的有效版本，不得再发起正式指派。
 */
@Entity
@Table(name = "published_plan_slot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedPlanSlot {

    /** 课程 ID，一门课程一个槽位 */
    @Id
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "active_plan_id", nullable = false)
    private Long activePlanId;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onWrite() {
        updatedAt = LocalDateTime.now();
    }
}
