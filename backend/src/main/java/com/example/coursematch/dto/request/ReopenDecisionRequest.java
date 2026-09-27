package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程负责人审批重开申请。批准后周期恢复到 RE_REVIEW（待复查），原结论保留为历史版本。
 */
@Data
public class ReopenDecisionRequest {

    /** APPROVED / REJECTED */
    @NotBlank(message = "审批结论不能为空")
    private String decision;

    @Size(max = 2000)
    private String comment;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
