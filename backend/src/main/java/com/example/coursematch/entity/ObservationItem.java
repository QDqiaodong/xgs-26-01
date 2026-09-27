package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察维度项：课堂组织 / 内容准确性 / 互动引导 / 时间控制。
 *
 * <p>每个维度保存事实、等级、改进建议三部分，每部分独立分级：
 * <ul>
 *   <li>sensitivityLevel：INTERNAL 内部判断（仅督导/负责人）、SENSITIVE 敏感（投诉原文、学员身份等，
 *       讲师看脱敏文）、IMPROVEMENT_RELATED 与本人改进相关（讲师可见）、PUBLIC 普通查看者可见的结论性内容</li>
 * </ul>
 *
 * <p>原始内容（original*）写入后永不静默修改：复核修正/撤销时原值复制到 {@link ObservationItemRevision}，
 * 当前生效值更新为 current*，并保留处理意见；后续改进目标只以当前有效项为准。
 */
@Entity
@Table(name = "observation_items",
        indexes = {
                @Index(name = "idx_obs_item_obs", columnList = "observation_id")
        })
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

    /** CLASSROOM_ORGANIZATION 课堂组织 / CONTENT_ACCURACY 内容准确性 /
     *  INTERACTION_GUIDANCE 互动引导 / TIME_CONTROL 时间控制 */
    @Column(name = "dimension", nullable = false, length = 30)
    private String dimension;

    // ---------------- 事实（原始 + 当前） ----------------
    @Lob
    @Column(name = "original_fact", nullable = false)
    private String originalFact;

    @Lob
    @Column(name = "current_fact", nullable = false)
    private String currentFact;

    /** 事实敏感分级：INTERNAL / SENSITIVE / IMPROVEMENT_RELATED / PUBLIC */
    @Column(name = "fact_level", nullable = false, length = 25)
    private String factLevel;

    /** 事实脱敏文本（服务端按敏感类型生成，讲师视图只下发该字段） */
    @Lob
    @Column(name = "masked_fact")
    private String maskedFact;

    // ---------------- 等级（原始 + 当前） ----------------
    /** 等级：A 优秀 / B 合格 / C 待改进 / D 不合格 */
    @Column(name = "original_grade", nullable = false, length = 2)
    private String originalGrade;

    @Column(name = "current_grade", nullable = false, length = 2)
    private String currentGrade;

    /** 等级是否为内部判断（内部判断讲师不可见原始理由，仅看脱敏后的改进建议） */
    @Column(name = "grade_internal", nullable = false)
    @Builder.Default
    private Boolean gradeInternal = false;

    // ---------------- 改进建议（原始 + 当前） ----------------
    @Lob
    @Column(name = "original_suggestion")
    private String originalSuggestion;

    @Lob
    @Column(name = "current_suggestion")
    private String currentSuggestion;

    @Column(name = "suggestion_level", length = 25)
    @Builder.Default
    private String suggestionLevel = "IMPROVEMENT_RELATED";

    // ---------------- 复核状态 ----------------
    /** ACTIVE 当前有效 / MODIFIED 经异议复核被修正（原值留痕）/ REVOKED 经复核被撤销 */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_review_round")
    private Integer lastReviewRound;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "ACTIVE";
        }
        if (gradeInternal == null) {
            gradeInternal = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
