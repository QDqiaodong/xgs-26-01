package com.example.coursematch.service;

import com.example.coursematch.dto.request.PlanCreateFromRecordRequest;
import com.example.coursematch.dto.response.PlanResponse;
import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;

import java.util.List;

/**
 * 推演方案版本管理：创建草稿快照、提交评审、双角色评审、驳回、发布、废弃、正式指派。
 */
public interface MatchPlanService {

    PlanResponse createDraft(PlanCreateFromRecordRequest request, String operator, String operatorName);

    PlanResponse updateCandidates(Long planId, List<Long> instructorIds, String operator, String operatorName);

    PlanResponse submit(Long planId, String operator, String operatorName);

    /** 讲师管理员核对讲师资质与可授课状态 */
    PlanResponse reviewByInstructorAdmin(Long planId, String decision, String comment,
                                        String operator, String operatorName);

    /** 课程管理员核对课程要求与采用的权重 */
    PlanResponse reviewByCourseAdmin(Long planId, String decision, String comment,
                                    String operator, String operatorName);

    /** 系统管理员驳回（不能替代专业评审通过） */
    PlanResponse reject(Long planId, String reason, String operator, String operatorName);

    PlanResponse publish(Long planId, String operator, String operatorName);

    void discard(Long planId, String operator, String operatorName);

    PlanResponse getPlan(Long id);

    List<PlanResponse> listPlans(Long courseId, String status);

    PlanSnapshotDiffResponse getDiff(Long planId);
}
