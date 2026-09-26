package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "course_industry", length = 50)
    private String courseIndustry;

    @Column(name = "weight_config_id", nullable = false)
    private Long weightConfigId;

    @Column(name = "teaching_years_weight")
    private Double teachingYearsWeight;

    @Column(name = "qualification_weight")
    private Double qualificationWeight;

    @Column(name = "rating_weight")
    private Double ratingWeight;

    @Column(name = "expertise_weight")
    private Double expertiseWeight;

    @Column(name = "style_weight")
    private Double styleWeight;

    @Column(name = "has_gap", nullable = false)
    private Boolean hasGap;

    @Column(name = "best_score")
    private Double bestScore;

    @Column(name = "match_count")
    private Integer matchCount;

    @Column(name = "batch_match")
    @Builder.Default
    private Boolean batchMatch = false;

    @Column(name = "batch_industry", length = 50)
    private String batchIndustry;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "operator", length = 50)
    @Builder.Default
    private String operator = "system";

    @Column(name = "algorithm_version", length = 20)
    @Builder.Default
    private String algorithmVersion = "1.0";

    @Column(length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (batchMatch == null) {
            batchMatch = false;
        }
        if (operator == null) {
            operator = "system";
        }
        if (algorithmVersion == null) {
            algorithmVersion = "1.0";
        }
    }
}