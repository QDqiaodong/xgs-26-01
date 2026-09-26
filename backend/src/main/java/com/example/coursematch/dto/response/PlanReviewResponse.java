package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlanReviewResponse {
    private Long id;
    /** INSTRUCTOR / COURSE */
    private String reviewType;
    /** APPROVED / REJECTED */
    private String decision;
    private String comment;
    private String reviewerUsername;
    private String reviewerName;
    private LocalDateTime createdAt;
}
