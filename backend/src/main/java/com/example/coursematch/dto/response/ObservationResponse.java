package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带教观察详情。服务端按当前登录角色做分级投影后下发：
 * <ul>
 *   <li>FULL：督导/课程负责人/管理员——原始内容、内部判断、投诉原文、完整时间线</li>
 *   <li>INSTRUCTOR：讲师本人——与改进相关的脱敏内容；学员身份/投诉原文/内部判断不可见</li>
 *   <li>PUBLIC：普通查看者——只有周期状态、进度与最终结论，看不到观察明细</li>
 * </ul>
 */
@Data
@Builder
public class ObservationResponse {

    private Long id;
    private Long courseId;
    private String courseName;
    private String sessionNo;
    private Long instructorId;
    private String instructorName;
    private LocalDateTime observedAt;
    private String observerUsername;
    private String observerName;
    private String title;
    private String status;
    private String statusLabel;
    private String attachmentSource;

    /** FULL / INSTRUCTOR / PUBLIC */
    private String viewLevel;

    /** 乐观锁版本：前端保存动作回传，服务端拒绝过期页面覆盖新结果 */
    private Long version;

    // FULL / INSTRUCTOR
    private String summary;

    private LocalDateTime ackDueAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private String instructorResponse;
    private LocalDateTime respondedAt;

    private LocalDateTime improvingSince;
    private String ownerUsername;
    private String ownerName;
    private LocalDateTime closedAt;

    private Integer ackRound;
    private Integer improveRound;
    private String rejectReason;
    private LocalDateTime extendUntil;
    private Integer extensionCount;
    private String escalateReason;

    private Integer disputeRoundsResolved;
    private Long reopenedFromConclusionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ItemView> items;
    private List<ItemRevisionView> itemRevisions;
    private List<AddendumView> addenda;
    private List<DisputeView> disputes;
    private List<GoalView> goals;
    private List<AttachmentView> attachments;
    private List<ConclusionView> conclusions;
    private List<ReopenView> reopenRequests;
    private List<EventView> events;
    private List<ReminderView> openReminders;

    @Data
    @Builder
    public static class ItemView {
        private Long id;
        private String dimension;
        private String dimensionLabel;
        /** 当前生效事实（按视图分级：FULL 原文，INSTRUCTOR 脱敏文，PUBLIC 不下发） */
        private String fact;
        private String factLevel;
        private String factLevelLabel;
        /** 是否为脱敏后的文本（讲师视图用于提示） */
        private Boolean masked;
        private String grade;
        private Boolean gradeInternal;
        private String suggestion;
        private String suggestionLevel;
        private String status;
        private Integer lastReviewRound;
        /** FULL 才下发：原始文字与原始等级，保证持续可追溯 */
        private String originalFact;
        private String originalGrade;
        private String originalSuggestion;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    public static class ItemRevisionView {
        private Long id;
        private Long itemId;
        private String dimension;
        private String action;
        private Integer reviewRound;
        private String previousFact;
        private String previousGrade;
        private String previousSuggestion;
        private String newFact;
        private String newGrade;
        private String newSuggestion;
        private String reviewOpinion;
        private String reviewerUsername;
        private String reviewerName;
        private Long disputeId;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class AddendumView {
        private Long id;
        private String content;
        private String sensitivityLevel;
        private String createdBy;
        private String createdByName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class DisputeView {
        private Long id;
        private Integer roundNo;
        private List<Long> itemIds;
        private String reason;
        private String raisedBy;
        private String raisedByName;
        private LocalDateTime raisedAt;
        private String status;
        private String resolution;
        private String resolutionOpinion;
        private String reviewerUsername;
        private String reviewerName;
        private LocalDateTime resolvedAt;
    }

    @Data
    @Builder
    public static class GoalView {
        private Long id;
        private Integer stageNo;
        private String title;
        private String description;
        private String criteria;
        private LocalDateTime dueDate;
        private String evidenceRequirement;
        private Boolean requiredFlag;
        private String status;
        private String createdBy;
        private String createdByName;
        private LocalDateTime achievedAt;
        private Long acceptedEvidenceId;
        private List<EvidenceView> evidences;
    }

    @Data
    @Builder
    public static class EvidenceView {
        private Long id;
        private String status;
        private String content;
        private String attachmentNames;
        private String submittedBy;
        private String submittedByName;
        private LocalDateTime submittedAt;
        private String reviewComment;
        private String reviewedBy;
        private String reviewedByName;
        private LocalDateTime reviewedAt;
        private String supplement;
        private LocalDateTime supplementedAt;
    }

    @Data
    @Builder
    public static class AttachmentView {
        private Long id;
        private String fileName;
        private String fileRef;
        private String sourceNote;
        private String sensitivityLevel;
        private String uploadedBy;
        private String uploadedByName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class ConclusionView {
        private Long id;
        private Integer versionNo;
        private String status;
        private String resultType;
        /** PUBLIC 也可看的结论性文字 */
        private String publicSummary;
        /** 仅 FULL */
        private String internalNote;
        private String decidedBy;
        private String decidedByName;
        private LocalDateTime decidedAt;
        private Long supersedesConclusionId;
        private Long supersededByConclusionId;
    }

    @Data
    @Builder
    public static class ReopenView {
        private Long id;
        private String reason;
        private String requestedBy;
        private String requestedByName;
        private LocalDateTime requestedAt;
        private String status;
        private String decisionComment;
        private String decidedBy;
        private String decidedByName;
        private LocalDateTime decidedAt;
    }

    @Data
    @Builder
    public static class EventView {
        private Long id;
        private String eventType;
        private String eventTypeLabel;
        private String actorUsername;
        private String actorName;
        private String fromStatus;
        private String toStatus;
        private String refType;
        private Long refId;
        private String detail;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class ReminderView {
        private Long id;
        private String reminderType;
        private String reminderTypeLabel;
        private String assigneeUsername;
        private String assigneeName;
        private String message;
        private LocalDateTime dueAt;
        private String status;
        private LocalDateTime createdAt;
    }
}
