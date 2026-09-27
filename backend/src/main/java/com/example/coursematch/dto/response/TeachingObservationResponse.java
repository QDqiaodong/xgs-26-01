package com.example.coursematch.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 带教观察详情按当前登录角色脱敏后的响应。 */
public record TeachingObservationResponse(
        Long id,
        Long courseId,
        Long assignmentId,
        String courseName,
        String sessionNo,
        Long instructorId,
        String instructorName,
        String observerName,
        String observerUsername,
        LocalDateTime occurredAt,
        String attachmentSource,
        String status,
        String statusText,
        boolean needsImprovement,
        String summary,
        String currentConclusion,
        Long currentConclusionId,
        Long versionLock,
        String viewLevel,
        LocalDateTime responseDueAt,
        LocalDateTime followupDueAt,
        LocalDateTime acknowledgedAt,
        String responseText,
        LocalDateTime respondedAt,
        LocalDateTime closedAt,
        List<ItemDto> items,
        List<RevisionDto> revisions,
        List<AddendumDto> addenda,
        List<DisputeDto> disputes,
        List<GoalDto> goals,
        List<DecisionDto> decisions,
        List<EventDto> timeline,
        List<ReminderDto> reminders
) {
    public record ItemDto(Long id, String dimension, String sensitivityLevel, String fact, String grade,
                          String advice, String effectiveStatus, Integer revisionNo, boolean masked) {}
    public record RevisionDto(Long id, Long itemId, Integer revisionNo, String dimension, String sensitivityLevel,
                              String fact, String grade, String advice, String changeType, String changedBy,
                              String changeReason, Long disputeId, LocalDateTime createdAt, boolean masked) {}
    public record AddendumDto(Long id, String sensitivityLevel, String content, String authorName,
                              LocalDateTime createdAt, boolean masked) {}
    public record DisputeDto(Long id, Long itemId, String reason, String raisedByName, LocalDateTime raisedAt,
                             String status, String reviewerName, String reviewComment, LocalDateTime reviewedAt) {}
    public record GoalDto(Long id, Integer stageNo, String title, String measurementCriteria, LocalDate dueDate,
                          Boolean required, String status, LocalDateTime activatedAt, LocalDateTime completedAt,
                          List<EvidenceDto> evidence) {}
    public record EvidenceDto(Long id, String content, String materialUri, String submittedByName,
                              LocalDateTime submittedAt, String status, String reviewComment,
                              String reviewedByName, LocalDateTime reviewedAt) {}
    public record DecisionDto(Long id, Integer versionNo, String decisionType, String conclusion, String basis,
                              String decidedByName, Long replacesDecisionId, String status, LocalDateTime createdAt) {}
    public record EventDto(Long id, String eventType, String sourceType, Long sourceId, String detail,
                           String actorName, LocalDateTime createdAt) {}
    public record ReminderDto(Long id, String reminderType, String assigneeRole, String title, String message,
                              String status, LocalDateTime dueAt, LocalDateTime createdAt) {}
}
