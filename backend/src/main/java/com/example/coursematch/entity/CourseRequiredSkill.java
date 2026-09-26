package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_required_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequiredSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 100)
    private String skillName;

    @Column(length = 50)
    private String skillType;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;

    @Column(name = "skill_level", length = 20)
    @Builder.Default
    private String skillLevel = "INTERMEDIATE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isMandatory == null) {
            isMandatory = true;
        }
        if (skillLevel == null) {
            skillLevel = "INTERMEDIATE";
        }
    }
}