package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_instructor_assignment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_assignment_source_plan", columnNames = {"source_plan_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInstructorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "instructor_name", length = 50)
    private String instructorName;

    @Column(name = "assignment_date")
    private LocalDate assignmentDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "operator", length = 50)
    @Builder.Default
    private String operator = "system";

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_by", length = 50)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---------------- 方案正式指派溯源 ----------------
    /** 来源已发布方案；非方案指派时为空，一个方案至多产生一条正式指派 */
    @Column(name = "source_plan_id")
    private Long sourcePlanId;

    @Column(name = "source_version_no")
    private Integer sourceVersionNo;

    /** 采用的候选排名（正式指派固定为第 1 名） */
    @Column(name = "source_candidate_rank")
    private Integer sourceCandidateRank;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
        if (assignmentDate == null) {
            assignmentDate = LocalDate.now();
        }
        if (createdBy == null) {
            createdBy = "system";
        }
        if (operator == null) {
            operator = "system";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}