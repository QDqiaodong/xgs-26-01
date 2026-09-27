package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 另一名督导对异议的复核决定。可以维持、修正或撤销具体观察项；
 * 原文字、原等级与处理意见通过修订留痕继续可追溯。
 */
@Data
public class DisputeReviewRequest {

    /** UPHELD 维持 / MODIFIED 修正 / REVOKED 撤销 / MIXED 部分修正 */
    @NotBlank(message = "复核结论不能为空")
    private String resolution;

    @NotBlank(message = "复核处理意见不能为空")
    @Size(max = 2000)
    private String opinion;

    /** 修正/撤销的具体观察项决定（UPHELD 时可不传） */
    private List<ItemDecision> itemDecisions;

    /**
     * 异议详情页的期望版本：两名督导先后处理同一异议时，
     * 已被处理的异议第二次提交将得到 409，而不是重复推进。
     */
    @NotNull(message = "缺少版本校验字段")
    private Long expectedVersion;

    @Data
    public static class ItemDecision {
        @NotNull(message = "观察项ID不能为空")
        private Long itemId;

        /** KEEP 维持该项 / MODIFY 修正该项 / REVOKE 撤销该项 */
        @NotBlank(message = "单项处理决定不能为空")
        private String action;

        /** action=MODIFY 时的新值 */
        private String newFact;
        private String newGrade;
        private String newSuggestion;
    }
}
