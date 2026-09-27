package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重开后督导完成复查，提交复查意见；随后由负责人基于复查结果再次结案形成新结论版本。
 */
@Data
public class RereviewRequest {

    @NotBlank(message = "复查意见不能为空")
    @Size(max = 2000)
    private String comment;

    /** 复查后是否仍需改进：true 回到 IMPROVING（保留原目标与证据继续推进），false 进入待结案 */
    @NotNull(message = "请明确复查后是否仍需改进")
    private Boolean improvementStillNeeded;

    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;
}
