package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 系统管理员驳回（可在评审中任意阶段驳回，但不能代替专业评审通过） */
@Data
public class PlanRejectRequest {

    @NotBlank(message = "驳回原因不能为空")
    private String reason;
}
