package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 观察列表项。明细字段不下发；普通查看者只能看到周期状态与最终结论性摘要。
 */
@Data
@Builder
public class ObservationListItemResponse {

    private Long id;
    private Long courseId;
    private String courseName;
    private String sessionNo;
    private Long instructorId;
    private String instructorName;
    private LocalDateTime observedAt;
    private String observerName;
    private String title;
    private String status;
    private String statusLabel;
    private String viewLevel;
    private Long version;

    /** 必需目标数 / 已取得有效证据的必需目标数（讲师与督导可见，PUBLIC 仅看到整体进度） */
    private Integer requiredGoalCount;
    private Integer achievedRequiredGoalCount;
    private Boolean allRequiredGoalsAchieved;

    /** 当前结论的公开摘要（所有视图可见） */
    private String currentConclusionSummary;
    private Integer conclusionVersionNo;

    /** 当前用户在此观察单上的待处理提醒类型（用于列表高亮） */
    private List<String> myPendingReminderTypes;

    private LocalDateTime createdAt;
}
