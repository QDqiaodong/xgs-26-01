package com.example.coursematch.service;

import com.example.coursematch.dto.response.PlanAssignmentResultResponse;

/**
 * 基于已发布方案的正式指派：发布后、正式指派前再次检查入选讲师当前容量与可用状态，
 * 通过才原子生成指派并回写方案；失败只记录原因，不产生半成品、不替换候选。
 */
public interface PlanAssignmentService {

    PlanAssignmentResultResponse assignFromPlan(Long planId, String operator, String operatorName);
}
