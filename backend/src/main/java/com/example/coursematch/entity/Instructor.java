package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "instructors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String certificates;

    @Column(name = "teaching_years", nullable = false)
    private Integer teachingYears;

    @Column(name = "student_rating", nullable = false)
    private Double studentRating;

    @Column(length = 500)
    private String expertiseAreas;

    @Column(length = 200)
    private String teachingStyle;

    @Column(length = 500)
    private String resume;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "available", nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "max_courses_per_month")
    @Builder.Default
    private Integer maxCoursesPerMonth = 4;

    @Column(name = "current_month_courses")
    @Builder.Default
    private Integer currentMonthCourses = 0;

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
        if (status == null) {
            status = "ACTIVE";
        }
        if (available == null) {
            available = true;
        }
        if (createdBy == null) {
            createdBy = "system";
        }
        if (maxCoursesPerMonth == null) {
            maxCoursesPerMonth = 4;
        }
        if (currentMonthCourses == null) {
            currentMonthCourses = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}