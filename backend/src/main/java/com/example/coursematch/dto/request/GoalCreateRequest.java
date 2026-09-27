package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分阶段改进目标。督导与讲师共同形成；每项必须有衡量标准、截止时间和证明材料要求。
 */
@Data
public class GoalCreateRequest {

    @NotNull(message = "阶段序号不能为空")
    private Integer stageNo;

    @NotBlank(message = "目标标题不能为空")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "衡量标准不能为空")
    @Size(max = 2000)
    private String criteria;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime dueDate;

    @NotBlank(message = "证明材料要求不能为空")
    @Size(max = 2000)
    private String evidenceRequirement;

    /** 是否必需目标（缺省为必需）；只有必需目标全部达成才能结案 */
    private Boolean requiredFlag;

    private Long expectedVersion;
}
