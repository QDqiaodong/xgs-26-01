package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 讲师在确认前对观察事实提出异议。
 */
@Data
public class DisputeRaiseRequest {

    /** 异议涉及的观察项ID（可空，表示对整体观察有异议） */
    private List<Long> itemIds;

    @NotBlank(message = "异议理由不能为空")
    @Size(max = 2000)
    private String reason;

    private Long expectedVersion;
}
