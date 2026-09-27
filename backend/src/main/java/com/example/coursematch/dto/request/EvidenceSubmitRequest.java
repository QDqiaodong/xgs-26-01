package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 讲师回报改进证据。
 */
@Data
public class EvidenceSubmitRequest {

    @NotBlank(message = "证据说明不能为空")
    @Size(max = 3000)
    private String content;

    /** 附件名称/链接清单 */
    @Size(max = 1000)
    private String attachmentNames;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
