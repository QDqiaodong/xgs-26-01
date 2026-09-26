package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlanCreateFromRecordRequest {

    /** 来源匹配推演记录 ID，方案快照基于该次推演结果固化 */
    @NotNull(message = "匹配推演记录ID不能为空")
    private Long matchRecordId;

    private String name;

    private String remark;
}
