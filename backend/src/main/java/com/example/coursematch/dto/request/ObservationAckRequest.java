package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 讲师确认已阅并回应。acknowledged=true 表示确认；
 * 拒绝确认走 acknowledge=false + rejectReason，观察单进入 INSTRUCTOR_REJECTED。
 */
@Data
public class ObservationAckRequest {

    @NotBlank(message = "请明确是否确认已阅")
    private String acknowledged;

    @Size(max = 2000)
    private String response;

    /** 拒绝确认理由（acknowledged=false 时必填） */
    @Size(max = 1000)
    private String rejectReason;

    /**
     * 讲师打开详情时看到的对象版本号。服务端比对当前版本，
     * 旧页面保存的过期内容不能覆盖较新的复核结果（不一致返回 409）。
     */
    private Long expectedVersion;

    public boolean isAcknowledged() {
        return "true".equalsIgnoreCase(acknowledged) || "YES".equalsIgnoreCase(acknowledged);
    }
}
