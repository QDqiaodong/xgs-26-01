package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 建立带教观察。督导按课程场次创建，可同时提交四个维度的观察项。
 */
@Data
public class ObservationCreateRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotBlank(message = "课程场次不能为空")
    @Size(max = 50)
    private String sessionNo;

    @NotNull(message = "讲师ID不能为空")
    private Long instructorId;

    @NotNull(message = "授课发生时间不能为空")
    private LocalDateTime observedAt;

    @NotBlank(message = "观察标题不能为空")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String summary;

    /** 附件来源说明 */
    @Size(max = 500)
    private String attachmentSource;

    /** 确认/回应期限（空则按系统默认时长） */
    private LocalDateTime ackDueAt;

    /** 维度观察项（至少一项） */
    @NotNull(message = "至少填写一个维度的观察项")
    @Size(min = 1, message = "至少填写一个维度的观察项")
    private List<ItemInput> items;

    /** 附件登记（可选） */
    private List<AttachmentInput> attachments;

    @Data
    public static class ItemInput {
        /** CLASSROOM_ORGANIZATION / CONTENT_ACCURACY / INTERACTION_GUIDANCE / TIME_CONTROL */
        @NotBlank(message = "观察维度不能为空")
        private String dimension;

        @NotBlank(message = "观察事实不能为空")
        private String fact;

        /** 事实分级：INTERNAL / SENSITIVE / IMPROVEMENT_RELATED / PUBLIC */
        @NotBlank(message = "事实分级不能为空")
        private String factLevel;

        /** 敏感类型（factLevel=SENSITIVE 时）：STUDENT_ID 学员身份 / COMPLAINT 投诉原文 / INTERNAL_JUDGEMENT 内部判断 */
        private String sensitiveType;

        @NotBlank(message = "等级不能为空")
        private String grade;

        private Boolean gradeInternal;

        private String suggestion;

        /** 建议分级，默认 IMPROVEMENT_RELATED */
        private String suggestionLevel;
    }

    @Data
    public static class AttachmentInput {
        @NotBlank(message = "附件名称不能为空")
        private String fileName;
        private String fileRef;
        private String sourceNote;
        /** 默认 INTERNAL */
        private String sensitivityLevel;
    }
}
