package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "weight_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teaching_years_weight", nullable = false)
    private Double teachingYearsWeight;

    @Column(name = "qualification_weight", nullable = false)
    private Double qualificationWeight;

    @Column(name = "rating_weight", nullable = false)
    private Double ratingWeight;

    @Column(name = "expertise_weight", nullable = false)
    private Double expertiseWeight;

    @Column(name = "style_weight", nullable = false)
    private Double styleWeight;

    @Column(name = "gap_threshold", nullable = false)
    private Double gapThreshold;

    @Column(nullable = false)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (isDefault == null) {
            isDefault = false;
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