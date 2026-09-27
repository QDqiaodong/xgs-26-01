package com.example.coursematch.service;

/**
 * 带教观察链路常量：状态、分级、维度、提醒类型、事件类型与中文文案。
 */
public final class ObservationConstants {

    private ObservationConstants() {
    }

    // ---------------- 观察单状态 ----------------
    public static final String ST_PENDING_ACK = "PENDING_ACK";
    public static final String ST_INSTRUCTOR_REJECTED = "INSTRUCTOR_REJECTED";
    public static final String ST_DISPUTED = "DISPUTED";
    public static final String ST_RESPONSE_OVERDUE = "RESPONSE_OVERDUE";
    public static final String ST_NO_IMPROVEMENT_NEEDED = "NO_IMPROVEMENT_NEEDED";
    public static final String ST_IMPROVING = "IMPROVING";
    public static final String ST_EVIDENCE_PENDING_REVIEW = "EVIDENCE_PENDING_REVIEW";
    public static final String ST_REVIEW_OVERDUE = "REVIEW_OVERDUE";
    public static final String ST_PENDING_OWNER_CLOSE = "PENDING_OWNER_CLOSE";
    public static final String ST_CLOSED = "CLOSED";
    public static final String ST_EXTENDED = "EXTENDED";
    public static final String ST_ESCALATED = "ESCALATED";
    public static final String ST_REOPEN_REQUESTED = "REOPEN_REQUESTED";
    public static final String ST_RE_REVIEW = "RE_REVIEW";

    // ---------------- 内容分级 ----------------
    public static final String LV_INTERNAL = "INTERNAL";
    public static final String LV_SENSITIVE = "SENSITIVE";
    public static final String LV_IMPROVEMENT = "IMPROVEMENT_RELATED";
    public static final String LV_PUBLIC = "PUBLIC";

    /** 敏感类型：学员身份 / 投诉原文 / 内部判断 */
    public static final String SENS_STUDENT_ID = "STUDENT_ID";
    public static final String SENS_COMPLAINT = "COMPLAINT";
    public static final String SENS_INTERNAL_JUDGEMENT = "INTERNAL_JUDGEMENT";

    // ---------------- 维度 ----------------
    public static final String DIM_CLASSROOM = "CLASSROOM_ORGANIZATION";
    public static final String DIM_CONTENT = "CONTENT_ACCURACY";
    public static final String DIM_INTERACTION = "INTERACTION_GUIDANCE";
    public static final String DIM_TIME = "TIME_CONTROL";

    // ---------------- 提醒类型 ----------------
    public static final String RMD_ACK_DUE = "ACK_DUE";
    public static final String RMD_RESPONSE_OVERDUE = "RESPONSE_OVERDUE";
    public static final String RMD_INSTRUCTOR_REJECTED = "INSTRUCTOR_REJECTED";
    public static final String RMD_DISPUTE_REVIEW = "DISPUTE_REVIEW";
    public static final String RMD_GOAL_PROGRESS = "GOAL_PROGRESS";
    public static final String RMD_EVIDENCE_REVIEW = "EVIDENCE_REVIEW";
    public static final String RMD_INFO_REQUESTED = "INFO_REQUESTED";
    public static final String RMD_EVIDENCE_REJECTED = "EVIDENCE_REJECTED";
    public static final String RMD_REVIEW_OVERDUE = "REVIEW_OVERDUE";
    public static final String RMD_OWNER_CLOSE = "OWNER_CLOSE";
    public static final String RMD_REOPEN_APPROVAL = "REOPEN_APPROVAL";
    public static final String RMD_RE_REVIEW = "RE_REVIEW";

    // ---------------- 角色 ----------------
    public static final String ROLE_SUPERVISOR = "SUPERVISOR";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_COURSE_ADMIN = "COURSE_ADMIN";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VIEWER = "VIEWER";

    public static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ST_PENDING_ACK -> "待讲师确认";
            case ST_INSTRUCTOR_REJECTED -> "讲师拒绝确认";
            case ST_DISPUTED -> "异议复核中";
            case ST_RESPONSE_OVERDUE -> "回应逾期";
            case ST_NO_IMPROVEMENT_NEEDED -> "待负责人结案（无需改进）";
            case ST_IMPROVING -> "改进中";
            case ST_EVIDENCE_PENDING_REVIEW -> "证据待复查";
            case ST_REVIEW_OVERDUE -> "督导复查逾期";
            case ST_PENDING_OWNER_CLOSE -> "待负责人结案";
            case ST_CLOSED -> "已结案";
            case ST_EXTENDED -> "周期已延长";
            case ST_ESCALATED -> "强化辅导";
            case ST_REOPEN_REQUESTED -> "重开申请中";
            case ST_RE_REVIEW -> "重开待复查";
            default -> status;
        };
    }

    public static String dimensionLabel(String dim) {
        if (dim == null) {
            return "";
        }
        return switch (dim) {
            case DIM_CLASSROOM -> "课堂组织";
            case DIM_CONTENT -> "内容准确性";
            case DIM_INTERACTION -> "互动引导";
            case DIM_TIME -> "时间控制";
            default -> dim;
        };
    }

    public static String levelLabel(String level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case LV_INTERNAL -> "内部判断（仅督导/负责人）";
            case LV_SENSITIVE -> "敏感（讲师看脱敏）";
            case LV_IMPROVEMENT -> "改进相关（讲师可见）";
            case LV_PUBLIC -> "公开结论";
            default -> level;
        };
    }

    public static String reminderLabel(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case RMD_ACK_DUE -> "待确认观察";
            case RMD_RESPONSE_OVERDUE -> "确认/回应逾期";
            case RMD_INSTRUCTOR_REJECTED -> "讲师拒绝确认";
            case RMD_DISPUTE_REVIEW -> "异议待复核";
            case RMD_GOAL_PROGRESS -> "待回报改进进展";
            case RMD_EVIDENCE_REVIEW -> "证据待复查";
            case RMD_INFO_REQUESTED -> "需补充证明材料";
            case RMD_EVIDENCE_REJECTED -> "证明不充分被退回";
            case RMD_REVIEW_OVERDUE -> "督导复查逾期";
            case RMD_OWNER_CLOSE -> "待负责人结案";
            case RMD_REOPEN_APPROVAL -> "重开申请待审批";
            case RMD_RE_REVIEW -> "重开待复查";
            default -> type;
        };
    }

    public static String eventLabel(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "CREATED" -> "建立观察";
            case "ADDENDUM_ADDED" -> "追加补充说明";
            case "ATTACHMENT_ADDED" -> "登记附件";
            case "ACKED" -> "讲师确认已阅";
            case "REJECTED_ACK" -> "讲师拒绝确认";
            case "RESPONDED" -> "讲师回应";
            case "DISPUTE_RAISED" -> "提出异议";
            case "DISPUTE_REVIEWED" -> "异议复核";
            case "STATUS_CHANGED" -> "状态推进";
            case "OVERDUE_MARKED" -> "标记逾期";
            case "GOAL_CREATED" -> "形成改进目标";
            case "EVIDENCE_SUBMITTED" -> "回报证据";
            case "EVIDENCE_REVIEWED" -> "复查证据";
            case "EVIDENCE_SUPPLEMENTED" -> "补充证明材料";
            case "CYCLE_CLOSED" -> "周期结案";
            case "CYCLE_EXTENDED" -> "延长周期";
            case "CYCLE_ESCALATED" -> "升级强化辅导";
            case "REOPEN_REQUESTED" -> "申请重开";
            case "REOPEN_DECIDED" -> "重开审批";
            case "RE_REVIEWED" -> "重开复查";
            default -> type;
        };
    }
}
