package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 正式指派结果。失败时 assignmentId 为空、success=false、reason 说明失败原因；
 * 失败不会产生半成品指派，也不会改用排名靠后的讲师，原因同步写入方案轨迹。
 */
@Data
@Builder
public class PlanAssignmentResultResponse {
    private Long planId;
    private Integer versionNo;
    private String status;
    private boolean success;
    private Long assignmentId;
    private Long instructorId;
    private String instructorName;
    private Integer candidateRank;
    private Double matchScore;
    private String reason;
    private LocalDateTime assignedAt;
}
