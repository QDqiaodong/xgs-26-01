package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 讲师针对“要求补充证据”的追加说明（不改变原回报内容）。
 */
@Data
public class EvidenceSupplementRequest {

    @NotBlank(message = "补充说明不能为空")
    @Size(max = 3000)
    private String supplement;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
