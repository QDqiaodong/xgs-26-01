package com.example.coursematch.service.impl;

import com.example.coursematch.dto.response.PlanAssignmentResultResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.PlanAssignmentService;
import com.example.coursematch.service.PlanConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanAssignmentServiceImpl implements PlanAssignmentService {

    private final MatchPlanRepository planRepository;
    private final MatchPlanCandidateRepository candidateRepository;
    private final PublishedPlanSlotRepository slotRepository;
    private final PlanEventRepository eventRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final OperationLogService operationLogService;

    @Override
    @Transactional(noRollbackFor = {IllegalArgumentException.class, PlanConflictException.class})
    public PlanAssignmentResultResponse assignFromPlan(Long planId, String operator, String operatorName) {
        // 1) 统一锁序：先锁该课程全部方案行，再锁发布槽位（与发布流程一致，避免交叉死锁）
        MatchPlan fetched = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId));
        List<MatchPlan> coursePlans = planRepository.findByCourseIdForUpdate(fetched.getCourseId());
        MatchPlan plan = coursePlans.stream().filter(p -> p.getId().equals(planId)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId));
        PublishedPlanSlot slot = slotRepository.findByCourseIdForUpdate(plan.getCourseId()).orElse(null);

        // 2) 只有当前有效发布版本才能发起正式指派；历史版本只可追溯
        if (slot == null || !planId.equals(slot.getActivePlanId())) {
            String reason = "该版本不是当前有效发布版本，不能发起正式指派";
            if (plan.getSupersededByPlanId() != null) {
                reason += "（已被 v" + plan.getSupersededByPlanId() + " 替代）";
            }
            recordFailure(plan, operator, operatorName, reason);
            throw new IllegalArgumentException(reason);
        }
        if (!"PUBLISHED".equals(plan.getStatus())) {
            String reason = "方案当前状态为 " + plan.getStatus() + "，不能发起正式指派";
            recordFailure(plan, operator, operatorName, reason);
            throw new IllegalArgumentException(reason);
        }

        // 3) 幂等：重复点击/超时重试，若已产生指派则直接返回既有结果，不生成重复指派
        if (plan.getAssignmentId() != null) {
            CourseInstructorAssignment existing =
                    assignmentRepository.findById(plan.getAssignmentId()).orElse(null);
            if (existing != null && !"CANCELLED".equals(existing.getStatus())) {
                throw new PlanConflictException("该方案已成功发起正式指派（指派 #" + existing.getId()
                        + "，讲师：" + existing.getInstructorName() + "），请勿重复操作");
            }
        }

        // 4) 只能采用方案快照中排名第一的候选，绝不偷偷改用排名靠后的讲师
        List<MatchPlanCandidate> candidates = candidateRepository.findByPlanIdOrderByRankNoAsc(planId);
        MatchPlanCandidate top = candidates.stream().filter(c -> c.getRankNo() != null && c.getRankNo() == 1)
                .findFirst().orElse(null);
        if (top == null) {
            String reason = "方案快照中缺少排名第一的候选讲师，无法指派";
            recordFailure(plan, operator, operatorName, reason);
            throw new IllegalArgumentException(reason);
        }

        // 5) 锁入选讲师行，发布后/指派前再次检查当前可用状态与本月容量（用锁内最新数据）
        Instructor instructor = instructorRepository.findByIdForUpdate(top.getInstructorId()).orElse(null);
        Course course = courseRepository.findById(plan.getCourseId()).orElse(null);
        if (course == null) {
            String reason = "课程已被删除，无法指派";
            recordFailure(plan, operator, operatorName, reason);
            throw new IllegalArgumentException(reason);
        }
        String failure = checkInstructorAssignable(plan, instructor, top.getInstructorName());
        if (failure != null) {
            // 不产生半成品指派、不替换候选，原因写入方案轨迹，供重新推演形成新版本
            recordFailure(plan, operator, operatorName, failure);
            operationLogService.log("PLAN_ASSIGN_FAIL", "MATCH_PLAN", planId,
                    "方案 v" + plan.getVersionNo() + " 正式指派失败：" + failure);
            return PlanAssignmentResultResponse.builder()
                    .planId(planId)
                    .versionNo(plan.getVersionNo())
                    .status(plan.getStatus())
                    .success(false)
                    .instructorId(top.getInstructorId())
                    .instructorName(top.getInstructorName())
                    .candidateRank(1)
                    .reason(failure)
                    .build();
        }

        // 6) 原子生成正式指派（source_plan_id 唯一约束兜底重复指派）
        assert instructor != null;
        boolean duplicated = assignmentRepository.existsByCourseIdAndInstructorIdAndStatus(
                course.getId(), instructor.getId(), "CONFIRMED");
        if (duplicated) {
            String reason = "讲师「" + instructor.getName() + "」已有该课程的进行中指派，不能重复指派";
            recordFailure(plan, operator, operatorName, reason);
            throw new PlanConflictException(reason);
        }

        CourseInstructorAssignment assignment = CourseInstructorAssignment.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .instructorId(instructor.getId())
                .instructorName(instructor.getName())
                .assignmentDate(LocalDate.now())
                .startDate(LocalDate.now())
                .matchScore(top.getTotalScore())
                .status("CONFIRMED")
                .sourcePlanId(planId)
                .sourceVersionNo(plan.getVersionNo())
                .sourceCandidateRank(1)
                .createdBy(operator)
                .updatedBy(operator)
                .remark("来自推演方案 v" + plan.getVersionNo() + " 的正式指派（第1名候选）")
                .build();
        assignment = assignmentRepository.save(assignment);
        assignmentRepository.flush();

        // 同步维护讲师本月计数，保持与普通“确认分配”一致
        instructor.setCurrentMonthCourses(
                (instructor.getCurrentMonthCourses() == null ? 0 : instructor.getCurrentMonthCourses()) + 1);
        instructorRepository.save(instructor);

        // 7) 回写方案：来源方案、版本、采用候选
        plan.setSelectedInstructorId(instructor.getId());
        plan.setSelectedInstructorName(instructor.getName());
        plan.setAssignmentId(assignment.getId());
        plan.setAssignedAt(LocalDateTime.now());
        planRepository.save(plan);

        eventRepository.save(PlanEvent.builder()
                .planId(planId)
                .eventType("ASSIGNMENT_SUCCEEDED")
                .actorUsername(operator)
                .actorName(operatorName)
                .sourceType("ASSIGNMENT")
                .sourceId(assignment.getId())
                .detail("正式指派成功：采用方案 v" + plan.getVersionNo() + " 排名第1候选「"
                        + instructor.getName() + "」，指派 #" + assignment.getId()
                        + "，已复核当前可用状态与本月容量")
                .build());
        operationLogService.log("PLAN_ASSIGN", "MATCH_PLAN", planId,
                "方案 v" + plan.getVersionNo() + " 正式指派讲师「" + instructor.getName() + "」");

        return PlanAssignmentResultResponse.builder()
                .planId(planId)
                .versionNo(plan.getVersionNo())
                .status(plan.getStatus())
                .success(true)
                .assignmentId(assignment.getId())
                .instructorId(instructor.getId())
                .instructorName(instructor.getName())
                .candidateRank(1)
                .matchScore(top.getTotalScore())
                .reason("正式指派成功")
                .assignedAt(plan.getAssignedAt())
                .build();
    }

    /**
     * 锁内复核讲师当前状态。容量计数以数据库中 CONFIRMED 且跨本月的指派为准，
     * 与方案快照无关（快照只用于评分/排序展示）。
     */
    private String checkInstructorAssignable(MatchPlan plan, Instructor instructor, String snapshotName) {
        if (instructor == null) {
            return "入选讲师「" + snapshotName + "」已被删除，请重新推演形成新版本";
        }
        if (!"ACTIVE".equals(instructor.getStatus())) {
            return "入选讲师「" + instructor.getName() + "」当前状态为 " + instructor.getStatus()
                    + "，不可授课，请重新推演形成新版本";
        }
        if (!Boolean.TRUE.equals(instructor.getAvailable())) {
            return "入选讲师「" + instructor.getName() + "」当前标记为不可用，请重新推演形成新版本";
        }
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        int used = assignmentRepository.countByInstructorIdAndStatusAndEndDateAfter(
                instructor.getId(), "CONFIRMED", monthStart);
        int capacity = instructor.getMaxCoursesPerMonth() == null ? 0 : instructor.getMaxCoursesPerMonth();
        if (used >= capacity) {
            return "入选讲师「" + instructor.getName() + "」本月课程容量已满（" + used + "/" + capacity
                    + "），请重新推演形成新版本";
        }
        return null;
    }

    private void recordFailure(MatchPlan plan, String operator, String operatorName, String reason) {
        eventRepository.save(PlanEvent.builder()
                .planId(plan.getId())
                .eventType("ASSIGNMENT_FAILED")
                .actorUsername(operator)
                .actorName(operatorName)
                .sourceType("ASSIGNMENT")
                .detail("正式指派失败：" + reason + "（未生成指派，仍采用方案内候选排序）")
                .build());
    }
}
