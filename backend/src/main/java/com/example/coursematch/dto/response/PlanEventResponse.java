package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 方案轨迹事件（评审轨迹/审计事实），只追加。
 */
@Data
@Builder
public class PlanEventResponse {
    private Long id;
    private String eventType;
    private String actorUsername;
    private String actorName;
    private String sourceType;
    private Long sourceId;
    private String detail;
    private LocalDateTime createdAt;
}
