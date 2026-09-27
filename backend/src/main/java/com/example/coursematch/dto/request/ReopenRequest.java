package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 结案后发现证据错误，发起重开申请。
 */
@Data
public class ReopenRequest {

    @NotBlank(message = "证据错误说明不能为空")
    @Size(max = 2000)
    private String reason;

    private Long expectedVersion;
}
