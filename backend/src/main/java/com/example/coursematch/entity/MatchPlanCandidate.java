package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 方案候选讲师快照：讲师关键资料 + 五项评分明细 + 排名。
 *
 * <p>方案提交评审后候选不可修改；这些数据在草稿创建时固化，
 * 之后讲师资料如何变化都不影响方案内的评分明细与排序。
 */
@Entity
@Table(name = "match_plan_candidates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_candidates_plan_instructor",
                        columnNames = {"plan_id", "instructor_id"})
        },
        indexes = {
                @Index(name = "idx_plan_candidates_plan_rank", columnList = "plan_id,rank_no")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlanCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    // ---------------- 讲师关键资料快照 ----------------
    @Column(name = "instructor_name", length = 50)
    private String instructorName;

    @Column(name = "instructor_status", length = 20)
    private String instructorStatus;

    @Column(name = "instructor_available")
    private Boolean instructorAvailable;

    @Column(name = "teaching_years")
    private Integer teachingYears;

    @Column(name = "student_rating")
    private Double studentRating;

    @Column(name = "certificates", length = 500)
    private String certificates;

    @Column(name = "expertise_areas", length = 500)
    private String expertiseAreas;

    @Column(name = "teaching_style", length = 200)
    private String teachingStyle;

    @Column(name = "skills_snapshot", columnDefinition = "TEXT")
    private String skillsSnapshot;

    @Column(name = "max_courses_per_month")
    private Integer maxCoursesPerMonth;

    // ---------------- 五项评分明细快照 ----------------
    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "teaching_years_score")
    private Double teachingYearsScore;

    @Column(name = "qualification_score")
    private Double qualificationScore;

    @Column(name = "rating_score")
    private Double ratingScore;

    @Column(name = "expertise_score")
    private Double expertiseScore;

    @Column(name = "style_score")
    private Double styleScore;

    /** 候选排序，1 为推演第一名；正式指派只允许采用 rank_no = 1 的候选 */
    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;
}
