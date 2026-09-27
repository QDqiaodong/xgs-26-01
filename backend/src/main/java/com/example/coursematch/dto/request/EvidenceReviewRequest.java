package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 督导复查证据：通过（ACCEPTED，成为有效证据）、退回（REJECTED，证明不充分）、
 * 要求补充（INFO_REQUESTED，讲师可在该证据上追加补充）。
 */
@Data
public class EvidenceReviewRequest {

    /** ACCEPTED / REJECTED / INFO_REQUESTED */
    @NotBlank(message = "复查结论不能为空")
    private String decision;

    @NotBlank(message = "复查意见不能为空")
    @Size(max = 2000)
    private String comment;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
