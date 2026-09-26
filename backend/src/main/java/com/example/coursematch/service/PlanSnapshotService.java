package com.example.coursematch.service;

import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;
import com.example.coursematch.entity.MatchPlan;

/**
 * 方案快照与当前资料的差异计算（只读），同时为发布前复核和失效检测提供依据。
 */
public interface PlanSnapshotService {

    PlanSnapshotDiffResponse buildDiff(MatchPlan plan);

    /**
     * 正式指派前复核当前入选（第 1 名）讲师：讲师存在、ACTIVE、available、本月容量未满。
     * 返回 null 表示通过；否则返回中文失败原因。
     */
    String checkTopCandidateAssignable(MatchPlan plan);
}
