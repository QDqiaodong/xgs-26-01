package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待处理提醒视图。提醒类型即责任分工：确认逾期、拒绝确认、异议待复核、证据待复查、
 * 复查逾期、负责人待结案、重开申请待审批等，各自独立，不合并为“处理中”。
 */
@Data
@Builder
public class ObservationReminderResponse {

    private Long id;
    private Long observationId;
    private String observationTitle;
    private String courseName;
    private String instructorName;
    private String reminderType;
    private String reminderTypeLabel;
    private String assigneeUsername;
    private String assigneeName;
    private String message;
    private String status;
    private LocalDateTime dueAt;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
