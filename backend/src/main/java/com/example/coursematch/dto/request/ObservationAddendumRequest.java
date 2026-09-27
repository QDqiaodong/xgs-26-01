package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 观察事后补充说明（只追加，不改原始记录）。
 */
@Data
public class ObservationAddendumRequest {

    @NotBlank(message = "补充内容不能为空")
    @Size(max = 2000)
    private String content;

    /** INTERNAL / SENSITIVE / IMPROVEMENT_RELATED / PUBLIC，默认 INTERNAL */
    private String sensitivityLevel;
}
