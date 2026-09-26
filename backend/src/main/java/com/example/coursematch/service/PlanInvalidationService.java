package com.example.coursematch.service;

import java.util.List;

/**
 * 资料变更失效联动。课程、入选讲师资质/可用状态/月度容量、方案使用权重在草稿创建后变化时，
 * 将受影响的未发布方案标记为 INVALIDATED，写明变化来源与字段明细，并阻止后续评审/发布。
 * 已发布方案不在失效范围（保留原始快照与审计事实），其正式指派前会单独做当前容量复核。
 */
public interface PlanInvalidationService {

    /** 课程要求变化（含必备技能集合变化） */
    void onCourseChanged(Long courseId, String changeSummary, List<String> changedFields);

    /** 讲师档案变化（资质相关字段、状态、可用状态） */
    void onInstructorChanged(Long instructorId, String instructorName,
                             String changeSummary, List<String> changedFields);

    /** 讲师月度容量变化（被确认的新指派占用容量） */
    void onInstructorCapacityChanged(Long instructorId, String instructorName, String detail);

    /** 权重配置变化 */
    void onWeightConfigChanged(Long weightConfigId, String changeSummary, List<String> changedFields);
}
