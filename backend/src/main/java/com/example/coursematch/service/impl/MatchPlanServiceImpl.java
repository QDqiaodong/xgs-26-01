package com.example.coursematch.service.impl;

import com.example.coursematch.dto.request.PlanCreateFromRecordRequest;
import com.example.coursematch.dto.response.PlanEventResponse;
import com.example.coursematch.dto.response.PlanResponse;
import com.example.coursematch.dto.response.PlanReviewResponse;
import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.service.MatchPlanService;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.PlanConflictException;
import com.example.coursematch.service.PlanSnapshotService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchPlanServiceImpl implements MatchPlanService {

    private final MatchPlanRepository planRepository;
    private final MatchPlanCandidateRepository candidateRepository;
    private final PlanReviewRepository reviewRepository;
    private final PlanEventRepository eventRepository;
    private final PublishedPlanSlotRepository slotRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final MatchRecordDetailRepository matchRecordDetailRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final WeightConfigRepository weightConfigRepository;
    private final InstructorSkillRepository instructorSkillRepository;
    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final PlanSnapshotService snapshotService;
    private final OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================ 创建草稿（固化快照） ============================

    @Override
    @Transactional
    public PlanResponse createDraft(PlanCreateFromRecordRequest request, String operator, String operatorName) {
        MatchRecord record = matchRecordRepository.findById(request.getMatchRecordId())
                .orElseThrow(() -> new EntityNotFoundException("匹配推演记录不存在: " + request.getMatchRecordId()));
        Course course = courseRepository.findById(record.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + record.getCourseId()));
        WeightConfig weight = weightConfigRepository.findById(record.getWeightConfigId()).orElse(null);

        // 版本号：同课程自增。锁住该课程的方案行，保证并发创建草稿也不撞版本号
        List<MatchPlan> locked = planRepository.findByCourseIdForUpdate(course.getId());
        int versionNo = (int) (planRepository.countByCourseId(course.getId()) + 1);

        MatchPlan plan = MatchPlan.builder()
                .matchRecordId(record.getId())
                .courseId(course.getId())
                .versionNo(versionNo)
                .name(request.getName() != null && !request.getName().isBlank()
                        ? request.getName()
                        : course.getName() + " 推演方案 v" + versionNo)
                .status("DRAFT")
                .courseName(course.getName())
                .courseIndustry(course.getIndustry())
                .courseDifficulty(course.getDifficulty())
                .courseRequiredQualifications(course.getRequiredQualifications())
                .courseStyleRequirements(course.getStyleRequirements())
                .courseStatus(course.getStatus())
                .weightConfigId(weight != null ? weight.getId() : record.getWeightConfigId())
                .weightConfigName(weight != null ? weight.getName() : null)
                .teachingYearsWeight(record.getTeachingYearsWeight())
                .qualificationWeight(record.getQualificationWeight())
                .ratingWeight(record.getRatingWeight())
                .expertiseWeight(record.getExpertiseWeight())
                .styleWeight(record.getStyleWeight())
                .gapThreshold(weight != null ? weight.getGapThreshold() : null)
                .bestScore(record.getBestScore())
                .createdBy(operator)
                .remark(request.getRemark())
                .build();
        plan = planRepository.save(plan);

        List<MatchRecordDetail> details = matchRecordDetailRepository.findByRecordIdOrderByRankAsc(record.getId());
        int count = 0;
        for (MatchRecordDetail d : details) {
            Instructor instructor = instructorRepository.findById(d.getInstructorId()).orElse(null);
            if (instructor == null) {
                continue;
            }
            candidateRepository.save(buildCandidate(plan.getId(), d, instructor));
            count++;
        }
        plan.setCandidateCount(count);
        planRepository.save(plan);

        addEvent(plan.getId(), "CREATED", operator, operatorName, null, null,
                "基于推演记录 #" + record.getId() + " 创建草稿 v" + versionNo + "，已固化课程要求、权重、"
                        + count + " 名候选讲师资料与五项评分排序");
        operationLogService.log("PLAN_CREATE", "MATCH_PLAN", plan.getId(),
                "创建推演方案 v" + versionNo + "（课程：" + course.getName() + "）");
        return getPlan(plan.getId());
    }

    private MatchPlanCandidate buildCandidate(Long planId, MatchRecordDetail d, Instructor instructor) {
        List<String> skillNames = instructorSkillRepository.findByInstructorId(instructor.getId())
                .stream().map(InstructorSkill::getSkillName).toList();
        return MatchPlanCandidate.builder()
                .planId(planId)
                .instructorId(instructor.getId())
                .instructorName(instructor.getName())
                .instructorStatus(instructor.getStatus())
                .instructorAvailable(instructor.getAvailable())
                .teachingYears(instructor.getTeachingYears())
                .studentRating(instructor.getStudentRating())
                .certificates(instructor.getCertificates())
                .expertiseAreas(instructor.getExpertiseAreas())
                .teachingStyle(instructor.getTeachingStyle())
                .skillsSnapshot(writeSkills(skillNames))
                .maxCoursesPerMonth(instructor.getMaxCoursesPerMonth())
                .totalScore(d.getTotalScore())
                .teachingYearsScore(d.getTeachingYearsScore())
                .qualificationScore(d.getQualificationScore())
                .ratingScore(d.getRatingScore())
                .expertiseScore(d.getExpertiseScore())
                .styleScore(d.getStyleScore())
                .rankNo(d.getRank())
                .build();
    }

    // ============================ 草稿候选调整（提交前可改） ============================

    @Override
    @Transactional
    public PlanResponse updateCandidates(Long planId, List<Long> instructorIds, String operator, String operatorName) {
        MatchPlan plan = lockPlan(planId);
        if (!"DRAFT".equals(plan.getStatus()) && !"REJECTED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("仅草稿或被驳回的方案可以调整候选，当前状态：" + plan.getStatus());
        }

        // 候选只能来自来源推演记录：按其当时五项得分与排名固化，不按最新资料重算
        List<MatchRecordDetail> details =
                matchRecordDetailRepository.findByRecordIdOrderByRankAsc(plan.getMatchRecordId());
        Map<Long, MatchRecordDetail> detailMap = new HashMap<>();
        for (MatchRecordDetail d : details) {
            detailMap.put(d.getInstructorId(), d);
        }

        // 只能选择/剔除候选，不能人为改变推演排名：按来源推演的原始 rank 升序固化
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(instructorIds));
        List<MatchPlanCandidate> rebuilt = new ArrayList<>();
        for (Long instructorId : distinctIds) {
            MatchRecordDetail d = detailMap.get(instructorId);
            if (d == null) {
                throw new IllegalArgumentException("讲师 " + instructorId + " 不在来源推演结果中，不能加入方案候选");
            }
            Instructor instructor = instructorRepository.findById(instructorId)
                    .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + instructorId));
            MatchPlanCandidate candidate = buildCandidate(plan.getId(), d, instructor);
            candidate.setRankNo(d.getRank()); // 排名始终沿用来源推演，不重新编号
            rebuilt.add(candidate);
        }
        if (rebuilt.isEmpty()) {
            throw new IllegalArgumentException("候选讲师不能为空");
        }
        rebuilt.sort(Comparator.comparing(MatchPlanCandidate::getRankNo));

        candidateRepository.deleteAll(candidateRepository.findByPlanIdOrderByRankNoAsc(plan.getId()));
        candidateRepository.flush();
        for (MatchPlanCandidate c : rebuilt) {
            candidateRepository.save(c);
        }
        plan.setCandidateCount(rebuilt.size());
        planRepository.save(plan);

        addEvent(plan.getId(), "CANDIDATES_UPDATED", operator, operatorName, null, null,
                "调整候选讲师为 " + rebuilt.size() + " 人，排序仍按来源推演得分固化");
        return getPlan(plan.getId());
    }

    // ============================ 提交评审 ============================

    @Override
    @Transactional(noRollbackFor = {IllegalArgumentException.class, PlanConflictException.class})
    public PlanResponse submit(Long planId, String operator, String operatorName) {
        MatchPlan plan = lockPlan(planId);
        if (!"DRAFT".equals(plan.getStatus()) && !"REJECTED".equals(plan.getStatus())) {
            if ("IN_REVIEW".equals(plan.getStatus()) || "APPROVED".equals(plan.getStatus())) {
                throw new PlanConflictException("方案已提交评审，请勿重复提交（方案 #" + planId + " v" + plan.getVersionNo() + "）");
            }
            throw new IllegalArgumentException("当前状态 " + plan.getStatus() + " 不能提交评审");
        }
        if (candidateRepository.countByPlanId(planId) == 0) {
            throw new IllegalArgumentException("方案没有候选讲师，不能提交评审");
        }
        // 提交时再复核一次资料变化，防止“改完资料立刻提交”绕过失效标记
        ensureNotStale(plan);

        plan.setStatus("IN_REVIEW");
        plan.setSubmittedBy(operator);
        plan.setSubmittedAt(LocalDateTime.now());
        // 驳回后重新提交：清空上一轮专业意见，避免旧意见被当作本轮结论
        reviewRepository.deleteByPlanId(planId);
        reviewRepository.flush();
        planRepository.save(plan);

        addEvent(plan.getId(), "SUBMITTED", operator, operatorName, null, null,
                "提交评审，候选与排序锁定，等待讲师管理员与课程管理员分别核对");
        operationLogService.log("PLAN_SUBMIT", "MATCH_PLAN", plan.getId(),
                "方案 v" + plan.getVersionNo() + " 提交评审");
        return getPlan(plan.getId());
    }

    // ============================ 双角色专业评审 ============================

    @Override
    @Transactional
    public PlanResponse reviewByInstructorAdmin(Long planId, String decision, String comment,
                                                String operator, String operatorName) {
        return review(planId, decision, comment, operator, operatorName, "INSTRUCTOR");
    }

    @Override
    @Transactional
    public PlanResponse reviewByCourseAdmin(Long planId, String decision, String comment,
                                           String operator, String operatorName) {
        return review(planId, decision, comment, operator, operatorName, "COURSE");
    }

    private PlanResponse review(Long planId, String decision, String comment, String operator,
                        String operatorName, String reviewType) {
        String normalized = normalizeDecision(decision);
        // 锁方案行，串行化两名管理员“同时通过最后一项评审”
        MatchPlan plan = lockPlan(planId);

        // 失效优先判断：失效后一律不得继续评审
        if ("INVALIDATED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("方案已因资料变化失效，不能继续评审。变化来源：" + plan.getInvalidReason());
        }

        Optional<PlanReview> existing = reviewRepository.findByPlanIdAndReviewType(planId, reviewType);
        if (existing.isPresent()) {
            // 重复点击/超时重试：明确告知该角色已有意见，不产生第二条记录
            throw new PlanConflictException("你已提交过" + reviewLabel(reviewType) + "意见（"
                    + existing.get().getDecision() + "），请勿重复提交");
        }

        if (!"IN_REVIEW".equals(plan.getStatus())) {
            if ("APPROVED".equals(plan.getStatus())) {
                throw new PlanConflictException("两类评审均已通过，方案等待发布，评审已结束");
            }
            if ("PUBLISHED".equals(plan.getStatus()) || "SUPERSEDED".equals(plan.getStatus())) {
                throw new PlanConflictException("方案已发布，评审已结束");
            }
            throw new IllegalArgumentException("仅评审中的方案可以提交评审意见，当前状态：" + plan.getStatus());
        }

        PlanReview review = PlanReview.builder()
                .planId(planId)
                .reviewType(reviewType)
                .decision(normalized)
                .comment(comment)
                .reviewerUsername(operator)
                .reviewerName(operatorName)
                .build();
        reviewRepository.save(review);
        reviewRepository.flush();

        if ("REJECTED".equals(normalized)) {
            plan.setStatus("REJECTED");
            planRepository.save(plan);
            addEvent(planId, "REJECTED", operator, operatorName, null, null,
                    reviewLabel(reviewType) + "驳回：" + safe(comment));
            operationLogService.log("PLAN_REVIEW_REJECT", "MATCH_PLAN", planId,
                    reviewLabel(reviewType) + "驳回方案 v" + plan.getVersionNo());
            return getPlan(planId);
        }

        addEvent(planId, "REVIEWED", operator, operatorName, null, null,
                reviewLabel(reviewType) + "通过" + (comment != null && !comment.isBlank() ? "：" + comment : ""));

        // 两类专业意见都通过才进入 APPROVED；系统管理员不参与，无法替代缺失的专业评审
        boolean instructorPassed = hasApproved(planId, "INSTRUCTOR");
        boolean coursePassed = hasApproved(planId, "COURSE");
        if (instructorPassed && coursePassed) {
            plan.setStatus("APPROVED");
            planRepository.save(plan);
            addEvent(planId, "REVIEWED", "system", "系统", null, null,
                    "讲师管理员与课程管理员两类评审均已通过，方案可发布");
        }
        operationLogService.log("PLAN_REVIEW_APPROVE", "MATCH_PLAN", planId,
                reviewLabel(reviewType) + "通过方案 v" + plan.getVersionNo());
        return getPlan(planId);
    }

    // ============================ 系统管理员驳回 ============================

    @Override
    @Transactional
    public PlanResponse reject(Long planId, String reason, String operator, String operatorName) {
        MatchPlan plan = lockPlan(planId);
        if ("PUBLISHED".equals(plan.getStatus()) || "SUPERSEDED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("已发布方案不能驳回，应重新推演形成新版本");
        }
        if ("INVALIDATED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("方案已失效，无需驳回");
        }
        plan.setStatus("REJECTED");
        planRepository.save(plan);
        addEvent(planId, "REJECTED", operator, operatorName, null, null,
                "系统管理员驳回：" + reason + "（驳回不替代任何专业评审意见）");
        operationLogService.log("PLAN_ADMIN_REJECT", "MATCH_PLAN", planId,
                "系统管理员驳回方案 v" + plan.getVersionNo() + "：" + reason);
        return getPlan(planId);
    }

    // ============================ 发布（并发唯一） ============================

    @Override
    @Transactional(noRollbackFor = {IllegalArgumentException.class, PlanConflictException.class})
    public PlanResponse publish(Long planId, String operator, String operatorName) {
        // 先锁该课程全部方案行，再锁发布槽位，保证同课程并发发布全局串行
        List<MatchPlan> coursePlans = planRepository.findByCourseIdForUpdate(
                planRepository.findById(planId)
                        .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId))
                        .getCourseId());
        MatchPlan plan = coursePlans.stream().filter(p -> p.getId().equals(planId)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId));

        PublishedPlanSlot slot = slotRepository.findByCourseIdForUpdate(plan.getCourseId()).orElse(null);

        // 幂等：重复点击/超时重试，若本方案已是有效发布版本，直接返回成功现状
        if (slot != null && planId.equals(slot.getActivePlanId())) {
            throw new PlanConflictException("方案 v" + plan.getVersionNo() + " 已发布成功，请勿重复发布");
        }
        if ("PUBLISHED".equals(plan.getStatus())) {
            // 本方案曾发布但槽位已指向别的版本
            throw new PlanConflictException("方案 v" + plan.getVersionNo()
                    + " 已被更新版本替代，不能再次发布");
        }
        if ("INVALIDATED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("方案已失效，不能发布。变化来源：" + plan.getInvalidReason());
        }
        if ("SUPERSEDED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("方案 v" + plan.getVersionNo() + " 已被替代，不能发布");
        }
        if (!"APPROVED".equals(plan.getStatus())) {
            if ("IN_REVIEW".equals(plan.getStatus())) {
                throw new IllegalArgumentException("两类专业评审尚未全部通过，不能发布");
            }
            throw new IllegalArgumentException("仅评审通过的方案可以发布，当前状态：" + plan.getStatus());
        }

        // 发布瞬间最后复核：评审通过后资料是否又变化
        PlanSnapshotDiffResponse diff = snapshotService.buildDiff(plan);
        if (diff.isHasAnyChange()) {
            // 直接置失效，指出变化来源，阻止本次发布
            String reason = "发布前复核发现快照与当前资料不一致，请重新推演形成新版本";
            plan.setStatus("INVALIDATED");
            plan.setInvalidSourceType("DATA");
            plan.setInvalidReason(reason);
            plan.setInvalidatedAt(LocalDateTime.now());
            planRepository.save(plan);
            addEvent(planId, "INVALIDATED", "system", "系统", "DATA", null, reason);
            throw new IllegalArgumentException(reason);
        }

        // 替代旧有效发布版本（旧版本可追溯，但不得再发起正式指派）
        MatchPlan previous = null;
        if (slot != null) {
            previous = planRepository.findByIdForUpdate(slot.getActivePlanId()).orElse(null);
            if (previous != null && "PUBLISHED".equals(previous.getStatus())) {
                previous.setStatus("SUPERSEDED");
                previous.setSupersededByPlanId(planId);
                planRepository.save(previous);
                addEvent(previous.getId(), "SUPERSEDED", operator, operatorName, null, null,
                        "被方案 v" + plan.getVersionNo() + " 替代，停止作为有效发布版本");
            }
        }

        plan.setStatus("PUBLISHED");
        plan.setPublishedBy(operator);
        plan.setPublishedAt(LocalDateTime.now());
        planRepository.save(plan);

        if (slot == null) {
            slot = PublishedPlanSlot.builder()
                    .courseId(plan.getCourseId())
                    .activePlanId(planId)
                    .updatedBy(operator)
                    .build();
        } else {
            slot.setActivePlanId(planId);
            slot.setUpdatedBy(operator);
        }
        slotRepository.save(slot);

        String suffix = previous != null ? "，旧发布版本 v" + previous.getVersionNo() + " 已转为历史版本" : "";
        addEvent(planId, "PUBLISHED", operator, operatorName, null, null,
                "方案发布为当前唯一有效版本" + suffix);
        operationLogService.log("PLAN_PUBLISH", "MATCH_PLAN", planId,
                "发布方案 v" + plan.getVersionNo() + suffix);
        return getPlan(planId);
    }

    // ============================ 废弃草稿 ============================

    @Override
    @Transactional
    public void discard(Long planId, String operator, String operatorName) {
        MatchPlan plan = lockPlan(planId);
        if ("PUBLISHED".equals(plan.getStatus()) || "SUPERSEDED".equals(plan.getStatus())) {
            throw new IllegalArgumentException("已发布版本不能删除，只能被新版本替代并保留追溯");
        }
        reviewRepository.deleteByPlanId(planId);
        candidateRepository.deleteAll(candidateRepository.findByPlanIdOrderByRankNoAsc(planId));
        eventRepository.deleteAll(eventRepository.findByPlanIdOrderByIdAsc(planId));
        operationLogService.log("PLAN_DISCARD", "MATCH_PLAN", planId,
                "废弃方案 v" + plan.getVersionNo());
        planRepository.delete(plan);
    }

    // ============================ 查询 ============================

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlan(Long id) {
        MatchPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + id));
        return toResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> listPlans(Long courseId, String status) {
        List<MatchPlan> plans;
        if (courseId != null) {
            plans = planRepository.findByCourseIdOrderByVersionNoDesc(courseId);
        } else if (status != null && !status.isBlank()) {
            plans = planRepository.findByStatusInOrderByCreatedAtDesc(List.of(status));
        } else {
            plans = planRepository.findAllByOrderByCreatedAtDesc();
        }
        Long activePlanId = courseId != null
                ? slotRepository.findById(courseId).map(PublishedPlanSlot::getActivePlanId).orElse(null)
                : null;
        return plans.stream()
                .filter(p -> status == null || status.isBlank() || status.equals(p.getStatus()))
                .map(p -> toResponse(p, activePlanId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSnapshotDiffResponse getDiff(Long planId) {
        MatchPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId));
        return snapshotService.buildDiff(plan);
    }

    // ============================ 内部方法 ============================

    private MatchPlan lockPlan(Long planId) {
        return planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new EntityNotFoundException("方案不存在: " + planId));
    }

    private void ensureNotStale(MatchPlan plan) {
        PlanSnapshotDiffResponse diff = snapshotService.buildDiff(plan);
        if (diff.isHasAnyChange()) {
            String reason = "提交前复核发现快照与当前资料不一致，请基于最新资料重新推演形成新版本";
            plan.setStatus("INVALIDATED");
            plan.setInvalidSourceType("DATA");
            plan.setInvalidReason(reason);
            plan.setInvalidatedAt(LocalDateTime.now());
            planRepository.save(plan);
            addEvent(plan.getId(), "INVALIDATED", "system", "系统", "DATA", null, reason);
            throw new IllegalArgumentException(reason);
        }
    }

    private boolean hasApproved(Long planId, String type) {
        return reviewRepository.findByPlanIdAndReviewType(planId, type)
                .map(r -> "APPROVED".equals(r.getDecision())).orElse(false);
    }

    private void addEvent(Long planId, String type, String username, String name,
                          String sourceType, Long sourceId, String detail) {
        eventRepository.save(PlanEvent.builder()
                .planId(planId)
                .eventType(type)
                .actorUsername(username)
                .actorName(name)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .detail(detail)
                .build());
    }

    private String normalizeDecision(String decision) {
        if (decision == null) {
            throw new IllegalArgumentException("评审结论不能为空");
        }
        String d = decision.trim().toUpperCase();
        if (!"APPROVED".equals(d) && !"REJECTED".equals(d)) {
            throw new IllegalArgumentException("评审结论只能是 APPROVED 或 REJECTED");
        }
        return d;
    }

    private String reviewLabel(String type) {
        return "INSTRUCTOR".equals(type) ? "讲师管理员（资质/可授课状态）" : "课程管理员（课程要求/权重）";
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "（未填写意见）" : s;
    }

    private PlanResponse toResponse(MatchPlan plan) {
        Long activePlanId = slotRepository.findById(plan.getCourseId())
                .map(PublishedPlanSlot::getActivePlanId).orElse(null);
        return toResponse(plan, activePlanId);
    }

    private PlanResponse toResponse(MatchPlan plan, Long activePlanId) {
        List<PlanReview> reviews = reviewRepository.findByPlanIdOrderByCreatedAtAsc(plan.getId());
        List<PlanEvent> events = eventRepository.findByPlanIdOrderByIdAsc(plan.getId());
        List<MatchPlanCandidate> candidates = candidateRepository.findByPlanIdOrderByRankNoAsc(plan.getId());

        boolean instructorPassed = hasApproved(plan.getId(), "INSTRUCTOR");
        boolean coursePassed = hasApproved(plan.getId(), "COURSE");

        List<PlanResponse.CandidateView> candidateViews = candidates.stream().map(c -> {
            Instructor current = instructorRepository.findById(c.getInstructorId()).orElse(null);
            boolean usable = false;
            String reason = null;
            if (current == null) {
                reason = "讲师已删除";
            } else if (!"ACTIVE".equals(current.getStatus())) {
                reason = "状态 " + current.getStatus();
            } else if (!Boolean.TRUE.equals(current.getAvailable())) {
                reason = "当前不可用";
            } else {
                int used = assignmentRepository.countByInstructorIdAndStatusAndEndDateAfter(
                        current.getId(), "CONFIRMED", LocalDate.now().withDayOfMonth(1));
                int cap = current.getMaxCoursesPerMonth() == null ? 0 : current.getMaxCoursesPerMonth();
                if (used >= cap) {
                    reason = "本月容量已满 " + used + "/" + cap;
                } else {
                    usable = true;
                }
            }
            return PlanResponse.CandidateView.builder()
                    .id(c.getId())
                    .instructorId(c.getInstructorId())
                    .instructorName(c.getInstructorName())
                    .instructorStatus(c.getInstructorStatus())
                    .instructorAvailable(c.getInstructorAvailable())
                    .teachingYears(c.getTeachingYears())
                    .studentRating(c.getStudentRating())
                    .certificates(c.getCertificates())
                    .expertiseAreas(c.getExpertiseAreas())
                    .teachingStyle(c.getTeachingStyle())
                    .skills(readSkills(c.getSkillsSnapshot()))
                    .maxCoursesPerMonth(c.getMaxCoursesPerMonth())
                    .totalScore(c.getTotalScore())
                    .teachingYearsScore(c.getTeachingYearsScore())
                    .qualificationScore(c.getQualificationScore())
                    .ratingScore(c.getRatingScore())
                    .expertiseScore(c.getExpertiseScore())
                    .styleScore(c.getStyleScore())
                    .rankNo(c.getRankNo())
                    .currentStatus(current != null ? current.getStatus() : null)
                    .currentAvailable(current != null ? current.getAvailable() : null)
                    .currentMaxCoursesPerMonth(current != null ? current.getMaxCoursesPerMonth() : null)
                    .currentUsableForAssignment(usable)
                    .currentUsableReason(reason)
                    .build();
        }).toList();

        return PlanResponse.builder()
                .id(plan.getId())
                .matchRecordId(plan.getMatchRecordId())
                .courseId(plan.getCourseId())
                .versionNo(plan.getVersionNo())
                .name(plan.getName())
                .status(plan.getStatus())
                .courseName(plan.getCourseName())
                .courseIndustry(plan.getCourseIndustry())
                .courseDifficulty(plan.getCourseDifficulty())
                .courseRequiredQualifications(plan.getCourseRequiredQualifications())
                .courseStyleRequirements(plan.getCourseStyleRequirements())
                .courseStatus(plan.getCourseStatus())
                .weightConfigId(plan.getWeightConfigId())
                .weightConfigName(plan.getWeightConfigName())
                .teachingYearsWeight(plan.getTeachingYearsWeight())
                .qualificationWeight(plan.getQualificationWeight())
                .ratingWeight(plan.getRatingWeight())
                .expertiseWeight(plan.getExpertiseWeight())
                .styleWeight(plan.getStyleWeight())
                .gapThreshold(plan.getGapThreshold())
                .bestScore(plan.getBestScore())
                .candidateCount(plan.getCandidateCount())
                .instructorReviewPassed(instructorPassed)
                .courseReviewPassed(coursePassed)
                .reviews(reviews.stream().map(this::toReviewResponse).toList())
                .invalidSourceType(plan.getInvalidSourceType())
                .invalidSourceId(plan.getInvalidSourceId())
                .invalidReason(plan.getInvalidReason())
                .invalidatedAt(plan.getInvalidatedAt())
                .activePublishedPlanId(activePlanId)
                .currentActivePublished(plan.getId().equals(activePlanId))
                .selectedInstructorId(plan.getSelectedInstructorId())
                .selectedInstructorName(plan.getSelectedInstructorName())
                .assignmentId(plan.getAssignmentId())
                .assignedAt(plan.getAssignedAt())
                .supersededByPlanId(plan.getSupersededByPlanId())
                .createdBy(plan.getCreatedBy())
                .submittedBy(plan.getSubmittedBy())
                .submittedAt(plan.getSubmittedAt())
                .publishedBy(plan.getPublishedBy())
                .publishedAt(plan.getPublishedAt())
                .remark(plan.getRemark())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .candidates(candidateViews)
                .events(events.stream().map(this::toEventResponse).toList())
                .build();
    }

    private PlanReviewResponse toReviewResponse(PlanReview r) {
        return PlanReviewResponse.builder()
                .id(r.getId())
                .reviewType(r.getReviewType())
                .decision(r.getDecision())
                .comment(r.getComment())
                .reviewerUsername(r.getReviewerUsername())
                .reviewerName(r.getReviewerName())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private PlanEventResponse toEventResponse(PlanEvent e) {
        return PlanEventResponse.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .actorUsername(e.getActorUsername())
                .actorName(e.getActorName())
                .sourceType(e.getSourceType())
                .sourceId(e.getSourceId())
                .detail(e.getDetail())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeSkills(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception e) {
            return "[]";
        }
    }
}
