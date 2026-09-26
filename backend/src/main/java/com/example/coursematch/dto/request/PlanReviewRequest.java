package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanReviewRequest {

    /** APPROVED / REJECTED */
    @NotBlank(message = "评审结论不能为空")
    private String decision;

    private String comment;
}
