package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 课程负责人周期决策：结案 / 延长周期 / 升级强化辅导。
 * 延长与升级必须写明依据；结案必须在必需目标都有有效证据后（或无需改进）才允许。
 */
@Data
public class CycleDecisionRequest {

    /** CLOSE 结案 / EXTEND 延长周期 / ESCALATE 升级强化辅导 */
    @NotBlank(message = "决策类型不能为空")
    private String decision;

    /** 结案时面向查看者的结论性文字（CLOSE 必填） */
    @Size(max = 2000)
    private String publicSummary;

    /** 结案结果类型：IMPROVED / NO_IMPROVEMENT_NEEDED */
    private String resultType;

    /** 内部结案说明（依据/证据概况） */
    @Size(max = 2000)
    private String internalNote;

    /** EXTEND / ESCALATE 时的书面依据（必填） */
    @Size(max = 2000)
    private String reason;

    /** 延长期限（EXTEND 时必填） */
    private java.time.LocalDateTime extendUntil;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
