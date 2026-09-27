package com.example/coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 周期结论版本。每次负责人结案/重开后再结案都新增一行，绝不覆盖旧结论：
 * 旧结论 status=SUPERSEDED 并由 supersededByConclusionId 指向替代它的新版本；
 * 新结论的 supersedesConclusionId 反向说明替代关系。
 *
 * <p>普通查看者只能看到当前结论的结论性文字，看不到观察明细。
 */
@Entity
@Table(name = "cycle_conclusions",
        indexes = @Index(name = "idx_conclusion_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CycleConclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** CLOSED 结案结论 / SUPERSEDED 被重开后的新结论替代 */
    @Column(nullable = false, length = 12)
    @Builder.Default
    private String status = "CLOSED";

    /** IMPROVED 改进达标结案 / NO_IMPROVEMENT_NEEDED 无需改进结案 */
    @Column(name = "result_type", nullable = false, length = 25)
    private String resultType;

    /** 面向普通查看者的结论性文字（公开层） */
    @Lob
    @Column(name = "public_summary", nullable = false)
    private String publicSummary;

    /** 内部结案说明（依据、证据概况；仅督导/负责人） */
    @Lob
    @Column(name = "internal_note")
    private String internalNote;

    @Column(name = "decided_by", nullable = false, length = 50)
    private String decidedBy;

    @Column(name = "decided_by_name", length = 50)
    private String decidedByName;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    /** 本结论替代的旧结论ID（重开后再结案时填写） */
    @Column(name = "supersedes_conclusion_id")
    private Long supersedesConclusionId;

    /** 本结论被哪个新结论替代 */
    @Column(name = "superseded_by_conclusion_id")
    private Long supersededByConclusionId;

    @PrePersist
    protected void onCreate() {
        decidedAt = LocalDateTime.now();
        if (status == null) {
            status = "CLOSED";
        }
    }
}
