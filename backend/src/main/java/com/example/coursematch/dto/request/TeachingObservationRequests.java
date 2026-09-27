package com.example.coursematch.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 讲师带教观察与改进周期的全部写接口请求；服务端按角色和当前状态再次校验。 */
@Data
public class TeachingObservationRequests {

    @Data
    public static class CreateObservation {
        @NotNull(message = "课程不能为空")
        private Long courseId;
        private Long assignmentId;
        @NotBlank(message = "课程场次不能为空")
        private String sessionNo;
        @NotNull(message = "讲师不能为空")
        private Long instructorId;
        @NotNull(message = "授课发生时间不能为空")
        private LocalDateTime occurredAt;
        private String attachmentSource;
        private String summary;
        private Boolean needsImprovement;
        @NotNull(message = "回应截止时间不能为空")
        private LocalDateTime responseDueAt;
        @Valid
        @NotEmpty(message = "至少记录一个观察维度")
        private List<ItemInput> items;
    }

    @Data
    public static class ItemInput {
        @NotBlank(message = "观察维度不能为空")
        private String dimension;
        @NotBlank(message = "敏感级别不能为空")
        private String sensitivityLevel;
        @NotBlank(message = "观察事实不能为空")
        private String fact;
        @NotBlank(message = "等级不能为空")
        private String grade;
        private String advice;
    }

    @Data
    public static class AddendumInput {
        @NotBlank(message = "敏感级别不能为空")
        private String sensitivityLevel;
        @NotBlank(message = "补充说明不能为空")
        private String content;
    }

    @Data
    public static class AcknowledgeRequest {
        private Long expectedVersion;
    }

    @Data
    public static class RespondRequest {
        @NotBlank(message = "回应内容不能为空")
        private String responseText;
        private Long expectedVersion;
    }

    @Data
    public static class RefuseRequest {
        @NotBlank(message = "拒绝确认原因不能为空")
        private String reason;
        private Long expectedVersion;
    }

    @Data
    public static class DisputeRequest {
        private Long itemId;
        @NotBlank(message = "异议理由不能为空")
        private String reason;
        private Long expectedVersion;
    }

    @Data
    public static class DisputeReviewRequest {
        @NotNull(message = "异议不能为空")
        private Long disputeId;
        /** UPHELD / CORRECTED / REVOKED */
        @NotBlank(message = "复核决定不能为空")
        private String decision;
        private String correctedFact;
        private String correctedGrade;
        private String correctedAdvice;
        private String correctedSensitivityLevel;
        @NotBlank(message = "复核意见不能为空")
        private String reviewComment;
        private Long expectedVersion;
    }

    @Data
    public static class GoalsRequest {
        @Valid
        @NotEmpty(message = "至少设置一个阶段目标")
        private List<GoalInput> goals;
        private Long expectedVersion;
    }

    @Data
    public static class GoalInput {
        @NotNull(message = "阶段序号不能为空")
        @Min(1)
        private Integer stageNo;
        @NotBlank(message = "目标标题不能为空")
        private String title;
        @NotBlank(message = "衡量标准不能为空")
        private String measurementCriteria;
        @NotNull(message = "截止日期不能为空")
        private LocalDate dueDate;
        private Boolean required;
        private Boolean activate;
    }

    @Data
    public static class EvidenceRequest {
        @NotBlank(message = "进展说明不能为空")
        private String content;
        private String materialUri;
        @NotBlank(message = "clientToken 不能为空，用于防重复提交")
        private String clientToken;
        private Long expectedVersion;
    }

    @Data
    public static class EvidenceReviewRequest {
        @NotNull(message = "证明不能为空")
        private Long evidenceId;
        /** SUPPLEMENT / REJECT / APPROVE */
        @NotBlank(message = "处理决定不能为空")
        private String decision;
        @NotBlank(message = "处理意见不能为空")
        private String comment;
        private LocalDate newDueDate;
        private Long expectedVersion;
    }

    @Data
    public static class DecisionRequest {
        @NotBlank(message = "依据不能为空")
        private String basis;
        private String conclusion;
        private LocalDate newDueDate;
        @NotBlank(message = "clientToken 不能为空，用于防重复点击")
        private String clientToken;
        private Long expectedVersion;
    }

    @Data
    public static class ReopenRequest {
        @NotBlank(message = "重开原因不能为空")
        private String reason;
        @NotBlank(message = "证据错误说明不能为空")
        private String evidenceError;
        @NotBlank(message = "clientToken 不能为空")
        private String clientToken;
        private Long expectedVersion;
    }

    @Data
    public static class ReopenDecisionRequest {
        @NotBlank(message = "批准意见不能为空")
        private String comment;
        private String clientToken;
        private Long expectedVersion;
    }

    @Data
    public static class NewConclusionRequest {
        @NotBlank(message = "新结论不能为空")
        private String conclusion;
        @NotBlank(message = "结论依据不能为空")
        private String basis;
        @NotBlank(message = "替代说明不能为空")
        private String replacementNote;
        @NotBlank(message = "clientToken 不能为空")
        private String clientToken;
        private Long expectedVersion;
    }
}
