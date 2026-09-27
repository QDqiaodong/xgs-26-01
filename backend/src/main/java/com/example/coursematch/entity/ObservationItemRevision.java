package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察项修订留痕。异议复核做出 MODIFIED / REVOKED 决定时，
 * 把该观察项被替换前的当前文字、等级与处理意见完整复制到本表，只追加不改写。
 *
 * <p>原观察人不能自行复核针对自己的异议，reviewer* 为另一名督导。
 */
@Entity
@Table(name = "observation_item_revisions",
        indexes = {
                @Index(name = "idx_obs_rev_item", columnList = "item_id"),
                @Index(name = "idx_obs_rev_obs", columnList = "observation_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationItemRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "dimension", nullable = false, length = 30)
    private String dimension;

    /** MODIFIED 修正 / REVOKED 撤销 */
    @Column(name = "action", nullable = false, length = 10)
    private String action;

    /** 异议轮次 */
    @Column(name = "review_round", nullable = false)
    private Integer reviewRound;

    // ---------------- 被替换前的值 ----------------
    @Lob
    @Column(name = "previous_fact")
    private String previousFact;

    @Column(name = "previous_grade", length = 2)
    private String previousGrade;

    @Lob
    @Column(name = "previous_suggestion")
    private String previousSuggestion;

    // ---------------- 修正后的值（REVOKED 时为空） ----------------
    @Lob
    @Column(name = "new_fact")
    private String newFact;

    @Column(name = "new_grade", length = 2)
    private String newGrade;

    @Lob
    @Column(name = "new_suggestion")
    private String newSuggestion;

    /** 复核处理意见（为什么维持/修正/撤销） */
    @Lob
    @Column(name = "review_opinion", nullable = false)
    private String reviewOpinion;

    @Column(name = "reviewer_username", nullable = false, length = 50)
    private String reviewerUsername;

    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(name = "dispute_id")
    private Long disputeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
