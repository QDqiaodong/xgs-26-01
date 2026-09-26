package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gap_warning_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapWarningRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ruleName;

    @Column(length = 500)
    private String description;

    @Column(name = "warning_level", nullable = false, length = 20)
    @Builder.Default
    private String warningLevel = "MEDIUM";

    @Column(name = "score_threshold", nullable = false)
    private Double scoreThreshold;

    @Column(name = "min_match_count")
    @Builder.Default
    private Integer minMatchCount = 1;

    @Column(name = "max_match_count")
    private Integer maxMatchCount;

    @Column(name = "target_industry", length = 50)
    private String targetIndustry;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (warningLevel == null) {
            warningLevel = "MEDIUM";
        }
        if (minMatchCount == null) {
            minMatchCount = 1;
        }
        if (createdBy == null) {
            createdBy = "system";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}