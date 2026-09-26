package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 方案详情（版本、快照、候选排序、双角色评审进度、轨迹、失效与发布/指派结果）。
 */
@Data
@Builder
public class PlanResponse {

    private Long id;
    private Long matchRecordId;
    private Long courseId;
    private Integer versionNo;
    private String name;
    private String status;

    // 课程快照
    private String courseName;
    private String courseIndustry;
    private String courseDifficulty;
    private String courseRequiredQualifications;
    private String courseStyleRequirements;
    private String courseStatus;

    // 权重快照
    private Long weightConfigId;
    private String weightConfigName;
    private Double teachingYearsWeight;
    private Double qualificationWeight;
    private Double ratingWeight;
    private Double expertiseWeight;
    private Double styleWeight;
    private Double gapThreshold;

    private Double bestScore;
    private Integer candidateCount;

    // 评审汇总
    private boolean instructorReviewPassed;
    private boolean courseReviewPassed;
    private List<PlanReviewResponse> reviews;

    // 失效信息
    private String invalidSourceType;
    private Long invalidSourceId;
    private String invalidReason;
    private LocalDateTime invalidatedAt;

    // 发布/指派结果
    private Long activePublishedPlanId;
    private boolean currentActivePublished;
    private Long selectedInstructorId;
    private String selectedInstructorName;
    private Long assignmentId;
    private LocalDateTime assignedAt;
    private Long supersededByPlanId;

    private String createdBy;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CandidateView> candidates;
    private List<PlanEventResponse> events;

    @Data
    @Builder
    public static class CandidateView {
        private Long id;
        private Long instructorId;
        private String instructorName;
        private String instructorStatus;
        private Boolean instructorAvailable;
        private Integer teachingYears;
        private Double studentRating;
        private String certificates;
        private String expertiseAreas;
        private String teachingStyle;
        private List<String> skills;
        private Integer maxCoursesPerMonth;

        private Double totalScore;
        private Double teachingYearsScore;
        private Double qualificationScore;
        private Double ratingScore;
        private Double expertiseScore;
        private Double styleScore;
        private Integer rankNo;

        // 当前资料（不参与重算，仅供差异展示）
        private String currentStatus;
        private Boolean currentAvailable;
        private Integer currentMaxCoursesPerMonth;
        private boolean currentUsableForAssignment;
        private String currentUsableReason;
    }
}
