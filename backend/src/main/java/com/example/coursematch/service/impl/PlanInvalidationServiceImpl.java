package com.example.coursematch.service.impl;

import com.example.coursematch.entity.MatchPlan;
import com.example.coursematch.entity.MatchPlanCandidate;
import com.example.coursematch.entity.PlanEvent;
import com.example.coursematch.repository.MatchPlanCandidateRepository;
import com.example.coursematch.repository.MatchPlanRepository;
import com.example.coursematch.repository.PlanEventRepository;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.PlanInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanInvalidationServiceImpl implements PlanInvalidationService {

    /** 会被资料变化失效的未发布状态 */
    private static final List<String> UNPUBLISHED_STATUSES =
            List.of("DRAFT", "IN_REVIEW", "APPROVED", "REJECTED");

    private final MatchPlanRepository planRepository;
    private final MatchPlanCandidateRepository candidateRepository;
    private final PlanEventRepository eventRepository;
    private final OperationLogService operationLogService;

    @Override
    @Transactional
    public void onCourseChanged(Long courseId, String changeSummary, List<String> changedFields) {
        List<MatchPlan> plans = planRepository.findByStatusIn(UNPUBLISHED_STATUSES).stream()
                .filter(p -> p.getCourseId().equals(courseId))
                .toList();
        String detail = buildDetail(changeSummary, changedFields);
        for (MatchPlan plan : plans) {
            invalidate(plan, "COURSE", courseId, "课程要求发生变化：" + detail);
        }
    }

    @Override
    @Transactional
    public void onInstructorChanged(Long instructorId, String instructorName,
                                    String changeSummary, List<String> changedFields) {
        List<Long> affectedPlanIds = candidateRepository.findByInstructorId(instructorId).stream()
                .map(MatchPlanCandidate::getPlanId)
                .distinct()
                .collect(Collectors.toList());
        if (affectedPlanIds.isEmpty()) {
            return;
        }
        String detail = buildDetail(changeSummary, changedFields);
        for (Long planId : affectedPlanIds) {
            planRepository.findById(planId).ifPresent(plan -> {
                if (UNPUBLISHED_STATUSES.contains(plan.getStatus())) {
                    invalidate(plan, "INSTRUCTOR", instructorId,
                            "入选讲师「" + instructorName + "」资料/资质发生变化：" + detail);
                }
            });
        }
    }

    @Override
    @Transactional
    public void onInstructorCapacityChanged(Long instructorId, String instructorName, String detail) {
        List<Long> affectedPlanIds = candidateRepository.findByInstructorId(instructorId).stream()
                // 只有入选（第一名）候选的容量变化才阻断方案
                .filter(c -> c.getRankNo() != null && c.getRankNo() == 1)
                .map(MatchPlanCandidate::getPlanId)
                .collect(Collectors.toList());
        for (Long planId : affectedPlanIds) {
            planRepository.findById(planId).ifPresent(plan -> {
                if (UNPUBLISHED_STATUSES.contains(plan.getStatus())) {
                    invalidate(plan, "CAPACITY", instructorId,
                            "入选讲师「" + instructorName + "」月度容量发生变化：" + detail);
                }
            });
        }
    }

    @Override
    @Transactional
    public void onWeightConfigChanged(Long weightConfigId, String changeSummary, List<String> changedFields) {
        List<MatchPlan> plans = planRepository.findByStatusIn(UNPUBLISHED_STATUSES).stream()
                .filter(p -> weightConfigId.equals(p.getWeightConfigId()))
                .toList();
        String detail = buildDetail(changeSummary, changedFields);
        for (MatchPlan plan : plans) {
            invalidate(plan, "WEIGHT", weightConfigId, "方案采用的权重配置发生变化：" + detail);
        }
    }

    private void invalidate(MatchPlan plan, String sourceType, Long sourceId, String reason) {
        plan.setStatus("INVALIDATED");
        plan.setInvalidSourceType(sourceType);
        plan.setInvalidSourceId(sourceId);
        plan.setInvalidReason(reason);
        plan.setInvalidatedAt(LocalDateTime.now());
        planRepository.save(plan);

        eventRepository.save(PlanEvent.builder()
                .planId(plan.getId())
                .eventType("INVALIDATED")
                .actorUsername("system")
                .actorName("系统")
                .sourceType(sourceType)
                .sourceId(sourceId)
                .detail(reason)
                .build());

        operationLogService.log("PLAN_INVALIDATE", "MATCH_PLAN", plan.getId(),
                "方案 v" + plan.getVersionNo() + " 失效：" + reason);
        log.info("Match plan {} invalidated by {} {}: {}", plan.getId(), sourceType, sourceId, reason);
    }

    private String buildDetail(String changeSummary, List<String> changedFields) {
        String fields = (changedFields == null || changedFields.isEmpty())
                ? "" : "（变化项：" + String.join("、", changedFields) + "）";
        return changeSummary + fields;
    }
}
