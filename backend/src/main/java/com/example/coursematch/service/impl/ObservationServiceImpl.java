package com.example.coursematch.service.impl;

import com.example.coursematch.dto.request.*;
import com.example.coursematch.dto.response.ObservationListItemResponse;
import com.example.coursematch.dto.response.ObservationReminderResponse;
import com.example.coursematch.dto.response.ObservationResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.ForbiddenException;
import com.example.coursematch.service.ObservationConflictException;
import com.example.coursematch.service.ObservationConstants;
import com.example.coursematch.service.ObservationService;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.SensitiveMaskingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.example.coursematch.service.ObservationConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private static final long DEFAULT_ACK_HOURS = 72;
    /** 督导复查证据的时限：超过该时长未复查标记复查逾期 */
    private static final long REVIEW_DUE_HOURS = 48;

    private final TeachingObservationRepository observationRepository;
    private final ObservationItemRepository itemRepository;
    private final ObservationItemRevisionRepository revisionRepository;
    private final ObservationDisputeRepository disputeRepository;
    private final ObservationAddendumRepository addendumRepository;
    private final ImprovementGoalRepository goalRepository;
    private final GoalEvidenceRepository evidenceRepository;
    private final CycleConclusionRepository conclusionRepository;
    private final ObservationReopenRequestRepository reopenRepository;
    private final ObservationAttachmentRepository attachmentRepository;
    private final ObservationEventRepository eventRepository;
    private final ObservationReminderRepository reminderRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final UserRepository userRepository;
    private final SensitiveMaskingService maskingService;
    private final OperationLogService operationLogService;

    // ============================ 建立观察 ============================

    @Override
    @Transactional
    public ObservationResponse create(ObservationCreateRequest request, AuthUser user) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + request.getCourseId()));
        Instructor instructor = instructorRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + request.getInstructorId()));
        validateDimensions(request.getItems());
        validateLevels(request.getItems());

        // 课程负责人取该课程体系的课程管理员（取首位启用账号，仅用于指派待办归属）
        User owner = userRepository.findEnabledByRoleName(ROLE_COURSE_ADMIN).stream().findFirst().orElse(null);

        LocalDateTime now = LocalDateTime.now();
        TeachingObservation obs = TeachingObservation.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .sessionNo(request.getSessionNo())
                .instructorId(instructor.getId())
                .instructorName(instructor.getName())
                .observedAt(request.getObservedAt())
                .observerUsername(user.getUsername())
                .observerName(user.getRealName())
                .attachmentSource(request.getAttachmentSource())
                .title(request.getTitle())
                .summary(request.getSummary())
                .status(ST_PENDING_ACK)
                .ackDueAt(request.getAckDueAt() != null ? request.getAckDueAt() : now.plusHours(DEFAULT_ACK_HOURS))
                .ackRound(1)
                .improveRound(0)
                .rejectCount(0)
                .extensionCount(0)
                .ownerUsername(owner != null ? owner.getUsername() : null)
                .ownerName(owner != null ? owner.getRealName() : null)
                .build();
        obs = observationRepository.save(obs);

        for (ObservationCreateRequest.ItemInput in : request.getItems()) {
            String grade = normalizeGrade(in.getGrade());
            boolean gradeInternal = Boolean.TRUE.equals(in.getGradeInternal()) || LV_INTERNAL.equals(in.getFactLevel());
            ObservationItem item = ObservationItem.builder()
                    .observationId(obs.getId())
                    .dimension(in.getDimension())
                    .originalFact(in.getFact())
                    .currentFact(in.getFact())
                    .factLevel(in.getFactLevel())
                    .maskedFact(buildMasked(in.getFact(), in.getFactLevel(), in.getSensitiveType()))
                    .originalGrade(grade)
                    .currentGrade(grade)
                    .gradeInternal(gradeInternal)
                    .originalSuggestion(in.getSuggestion())
                    .currentSuggestion(in.getSuggestion())
                    .suggestionLevel(in.getSuggestionLevel() != null ? in.getSuggestionLevel() : LV_IMPROVEMENT)
                    .status("ACTIVE")
                    .build();
            itemRepository.save(item);
        }

        if (request.getAttachments() != null) {
            for (ObservationCreateRequest.AttachmentInput a : request.getAttachments()) {
                attachmentRepository.save(ObservationAttachment.builder()
                        .observationId(obs.getId())
                        .fileName(a.getFileName())
                        .fileRef(a.getFileRef())
                        .sourceNote(a.getSourceNote())
                        .sensitivityLevel(a.getSensitivityLevel() != null ? a.getSensitivityLevel() : LV_INTERNAL)
                        .uploadedBy(user.getUsername())
                        .uploadedByName(user.getRealName())
                        .build());
            }
        }

        addEvent(obs.getId(), "CREATED", user, null, ST_PENDING_ACK, null, null,
                "按课程场次「" + course.getName() + " " + request.getSessionNo() + "」建立带教观察，被观察讲师："
                        + instructor.getName() + "，维度项 " + request.getItems().size() + " 条");
        remind(obs, RMD_ACK_DUE, "ack-" + obs.getAckRound(),
                instructorUser(instructor.getId()), instructor.getName(),
                "请于 " + obs.getAckDueAt() + " 前确认已阅带教观察并作出回应", obs.getAckDueAt());
        operationLogService.log("OBSERVATION_CREATE", "TEACHING_OBSERVATION", obs.getId(),
                "建立带教观察：" + request.getTitle() + "（讲师：" + instructor.getName() + "）");
        return getDetail(obs.getId(), user);
    }

    // ============================ 事后补充（只追加） ============================

    @Override
    @Transactional
    public ObservationResponse addAddendum(Long observationId, ObservationAddendumRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        // 结案后也允许追加说明，但不改变结论；追加永远不回写原始观察
        ObservationAddendum addendum = ObservationAddendum.builder()
                .observationId(obs.getId())
                .content(request.getContent())
                .sensitivityLevel(request.getSensitivityLevel() != null ? request.getSensitivityLevel() : LV_INTERNAL)
                .createdBy(user.getUsername())
                .createdByName(user.getRealName())
                .build();
        addendumRepository.save(addendum);
        addEvent(obs.getId(), "ADDENDUM_ADDED", user, obs.getStatus(), obs.getStatus(),
                "ADDENDUM", addendum.getId(), "追补充说明（原始观察内容未改动）");
        return getDetail(obs.getId(), user);
    }

    // ============================ 讲师确认 / 拒绝 ============================

    @Override
    @Transactional
    public ObservationResponse acknowledge(Long observationId, ObservationAckRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireInstructor(obs, user);
        checkVersion(obs, request.getExpectedVersion());

        String from = obs.getStatus();
        boolean allowed = ST_PENDING_ACK.equals(from) || ST_RESPONSE_OVERDUE.equals(from)
                || ST_INSTRUCTOR_REJECTED.equals(from);
        if (!allowed) {
            throw new ObservationConflictException("当前状态为「" + statusLabel(from)
                    + "」，不能再确认；请刷新查看最新状态");
        }

        if (request.isAcknowledged()) {
            LocalDateTime now = LocalDateTime.now();
            obs.setAcknowledgedAt(now);
            obs.setAcknowledgedBy(user.getUsername());
            obs.setInstructorResponse(request.getResponse());
            obs.setRespondedAt(now);

            // 是否需要改进：以“当前有效项”是否存在待改进及以下等级（C/D）为准
            boolean needImprovement = activeItems(obs.getId()).stream()
                    .anyMatch(i -> "C".equals(i.getCurrentGrade()) || "D".equals(i.getCurrentGrade()));
            resolveReminders(obs.getId(), Set.of(RMD_ACK_DUE, RMD_RESPONSE_OVERDUE, RMD_INSTRUCTOR_REJECTED),
                    "讲师已确认已阅");

            if (needImprovement) {
                enterImproving(obs, from, user, "讲师确认已阅，存在待改进维度，进入分阶段改进");
            } else {
                obs.setStatus(ST_NO_IMPROVEMENT_NEEDED);
                observationRepository.save(obs);
                addEvent(obs.getId(), "ACKED", user, from, ST_NO_IMPROVEMENT_NEEDED, null, null,
                        "讲师确认已阅并回应，当前有效项无 C/D 等级，无需改进，转课程负责人结案");
                remindOwnerClose(obs, "观察无需改进，等待课程负责人结案");
            }
        } else {
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new IllegalArgumentException("拒绝确认时必须填写理由");
            }
            obs.setRejectReason(request.getRejectReason());
            obs.setRejectCount(obs.getRejectCount() + 1);
            obs.setStatus(ST_INSTRUCTOR_REJECTED);
            observationRepository.save(obs);
            resolveReminders(obs.getId(), Set.of(RMD_ACK_DUE, RMD_RESPONSE_OVERDUE), "讲师拒绝确认");
            addEvent(obs.getId(), "REJECTED_ACK", user, from, ST_INSTRUCTOR_REJECTED, null, null,
                    "讲师拒绝确认：" + request.getRejectReason());
            remindSupervisors(obs, RMD_INSTRUCTOR_REJECTED, "reject-" + obs.getRejectCount(),
                    "讲师拒绝确认观察 #" + obs.getId() + "，请处理（与讲师沟通或等待其提出事实异议）");
        }
        return getDetail(obs.getId(), user);
    }

    // ============================ 异议：提出与隔离复核 ============================

    @Override
    @Transactional
    public ObservationResponse raiseDispute(Long observationId, DisputeRaiseRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireInstructor(obs, user);
        checkVersion(obs, request.getExpectedVersion());

        String from = obs.getStatus();
        boolean allowed = ST_PENDING_ACK.equals(from) || ST_RESPONSE_OVERDUE.equals(from)
                || ST_INSTRUCTOR_REJECTED.equals(from);
        if (!allowed) {
            throw new ObservationConflictException("异议只能在确认前提出，当前状态：" + statusLabel(from));
        }

        int round = obs.getDisputeRoundsResolved() + 1;
        Optional<ObservationDispute> dup = disputeRepository.findByObservationIdAndRoundNo(obs.getId(), round);
        if (dup.isPresent() && "OPEN".equals(dup.get().getStatus())) {
            throw new ObservationConflictException("本轮异议已提出，正在等待另一名督导复核，请勿重复提交");
        }

        // 校验异议涉及的观察项属于本单
        String itemIds = null;
        if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
            Set<Long> valid = new HashSet<>(itemRepository.findByObservationIdOrderByIdAsc(obs.getId())
                    .stream().map(ObservationItem::getId).toList());
            for (Long itemId : request.getItemIds()) {
                if (!valid.contains(itemId)) {
                    throw new IllegalArgumentException("观察项 #" + itemId + " 不属于本观察单");
                }
            }
            itemIds = request.getItemIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(null);
        }

        ObservationDispute dispute = ObservationDispute.builder()
                .observationId(obs.getId())
                .roundNo(round)
                .itemIds(itemIds)
                .reason(request.getReason())
                .raisedBy(user.getUsername())
                .raisedByName(user.getRealName())
                .status("OPEN")
                .build();
        dispute = disputeRepository.save(dispute);

        obs.setStatus(ST_DISPUTED);
        observationRepository.save(obs);
        resolveReminders(obs.getId(), Set.of(RMD_ACK_DUE, RMD_RESPONSE_OVERDUE, RMD_INSTRUCTOR_REJECTED),
                "讲师提出事实异议");
        addEvent(obs.getId(), "DISPUTE_RAISED", user, from, ST_DISPUTED, "DISPUTE", dispute.getId(),
                "讲师提出第 " + round + " 轮异议，等待另一名教学督导复核");
        // 广播给全体督导；原观察人在服务端被排除复核资格
        remindSupervisors(obs, RMD_DISPUTE_REVIEW, "dispute-" + round,
                "观察 #" + obs.getId() + " 第 " + round + " 轮异议待复核（原观察人不得自行复核）");
        operationLogService.log("OBSERVATION_DISPUTE", "TEACHING_OBSERVATION", obs.getId(),
                "讲师对观察 #" + obs.getId() + " 提出第 " + round + " 轮异议");
        return getDetail(obs.getId(), user);
    }

    @Override
    @Transactional
    public ObservationResponse reviewDispute(Long observationId, Long disputeId,
                                             DisputeReviewRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        checkVersion(obs, request.getExpectedVersion());
        if (!ST_DISPUTED.equals(obs.getStatus())) {
            throw new ObservationConflictException("当前不在异议复核中，状态：" + statusLabel(obs.getStatus()));
        }
        ObservationDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new EntityNotFoundException("异议不存在: " + disputeId));
        if (!dispute.getObservationId().equals(obs.getId())) {
            throw new IllegalArgumentException("异议与观察单不匹配");
        }
        if (!"OPEN".equals(dispute.getStatus())) {
            // 两名督导先后处理同一异议：后来者得到明确 409，不重复推进/留痕
            throw new ObservationConflictException("该异议已由 " + dispute.getReviewerName()
                    + " 完成复核（结论：" + dispute.getResolution() + "），请勿重复处理");
        }
        // 原观察人不能自行关闭针对自己的异议
        if (user.getUsername().equals(obs.getObserverUsername())) {
            throw new ForbiddenException("原观察人不能自行复核针对本人观察的异议，请由另一名教学督导处理");
        }

        String resolution = normalizeDisputeResolution(request.getResolution());
        List<ObservationItem> items = itemRepository.findByObservationIdOrderByIdAsc(obs.getId());
        Map<Long, ObservationItem> itemMap = new HashMap<>();
        items.forEach(i -> itemMap.put(i.getId(), i));

        boolean anyModified = false;
        boolean anyRevoked = false;
        if (request.getItemDecisions() != null) {
            for (DisputeReviewRequest.ItemDecision d : request.getItemDecisions()) {
                ObservationItem item = itemMap.get(d.getItemId());
                if (item == null) {
                    throw new IllegalArgumentException("观察项 #" + d.getItemId() + " 不属于本观察单");
                }
                String action = d.getAction() == null ? "" : d.getAction().trim().toUpperCase();
                if ("KEEP".equals(action)) {
                    continue;
                }
                if ("MODIFY".equals(action)) {
                    applyItemModification(obs, item, d, dispute, request, user);
                    anyModified = true;
                } else if ("REVOKE".equals(action)) {
                    applyItemRevoke(obs, item, dispute, request, user);
                    anyRevoked = true;
                } else {
                    throw new IllegalArgumentException("单项处理决定只能是 KEEP / MODIFY / REVOKE");
                }
            }
        }
        String effectiveResolution = resolution;
        if ("MIXED".equals(resolution) || (anyModified && anyRevoked)) {
            effectiveResolution = "MIXED";
        } else if (anyRevoked && !anyModified) {
            effectiveResolution = "REVOKED";
        } else if (anyModified && !anyRevoked) {
            effectiveResolution = "MODIFIED";
        }

        dispute.setStatus("RESOLVED");
        dispute.setResolution(effectiveResolution);
        dispute.setResolutionOpinion(request.getOpinion());
        dispute.setReviewerUsername(user.getUsername());
        dispute.setReviewerName(user.getRealName());
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        // 回到待确认，由讲师基于“当前有效项”重新确认；新一轮 ack 提醒（新 dedup key）
        obs.setDisputeRoundsResolved(dispute.getRoundNo());
        obs.setAckRound(obs.getAckRound() + 1);
        obs.setAckDueAt(LocalDateTime.now().plusHours(DEFAULT_ACK_HOURS));
        String from = obs.getStatus();
        obs.setStatus(ST_PENDING_ACK);
        obs.setAcknowledgedAt(null);
        obs.setAcknowledgedBy(null);
        observationRepository.save(obs);

        resolveReminders(obs.getId(), Set.of(RMD_DISPUTE_REVIEW), "异议已由督导 " + user.getRealName() + " 复核");
        addEvent(obs.getId(), "DISPUTE_REVIEWED", user, from, ST_PENDING_ACK, "DISPUTE", dispute.getId(),
                "第 " + dispute.getRoundNo() + " 轮异议复核结论：" + effectiveResolution
                        + "；原文字/原等级已留痕，等待讲师按当前有效项重新确认");
        User instructorUser = instructorUser(obs.getInstructorId());
        remind(obs, RMD_ACK_DUE, "ack-" + obs.getAckRound(), instructorUser, obs.getInstructorName(),
                "异议已由另一名督导复核，请查看当前有效观察项并于 " + obs.getAckDueAt() + " 前重新确认",
                obs.getAckDueAt());
        operationLogService.log("OBSERVATION_DISPUTE_REVIEW", "TEACHING_OBSERVATION", obs.getId(),
                "督导 " + user.getRealName() + " 复核观察 #" + obs.getId() + " 第 " + dispute.getRoundNo() + " 轮异议："
                        + effectiveResolution);
        return getDetail(obs.getId(), user);
    }

    private void applyItemModification(TeachingObservation obs, ObservationItem item,
                                       DisputeReviewRequest.ItemDecision d, ObservationDispute dispute,
                                       DisputeReviewRequest request, AuthUser reviewer) {
        String newFact = d.getNewFact() != null && !d.getNewFact().isBlank() ? d.getNewFact() : item.getCurrentFact();
        String newGrade = d.getNewGrade() != null && !d.getNewGrade().isBlank()
                ? normalizeGrade(d.getNewGrade()) : item.getCurrentGrade();
        String newSuggestion = d.getNewSuggestion();

        ObservationItemRevision rev = ObservationItemRevision.builder()
                .observationId(obs.getId())
                .itemId(item.getId())
                .dimension(item.getDimension())
                .action("MODIFIED")
                .reviewRound(dispute.getRoundNo())
                .previousFact(item.getCurrentFact())
                .previousGrade(item.getCurrentGrade())
                .previousSuggestion(item.getCurrentSuggestion())
                .newFact(newFact)
                .newGrade(newGrade)
                .newSuggestion(newSuggestion)
                .reviewOpinion(request.getOpinion())
                .reviewerUsername(reviewer.getUsername())
                .reviewerName(reviewer.getRealName())
                .disputeId(dispute.getId())
                .build();
        revisionRepository.save(rev);

        item.setCurrentFact(newFact);
        item.setCurrentGrade(newGrade);
        item.setCurrentSuggestion(newSuggestion);
        item.setStatus("MODIFIED");
        item.setLastReviewRound(dispute.getRoundNo());
        // 修正后的事实按原分级重新生成脱敏文本
        item.setMaskedFact(buildMasked(newFact, item.getFactLevel(), sensitiveTypeOf(item.getFactLevel())));
        itemRepository.save(item);
    }

    private void applyItemRevoke(TeachingObservation obs, ObservationItem item,
                                 ObservationDispute dispute, DisputeReviewRequest request, AuthUser reviewer) {
        ObservationItemRevision rev = ObservationItemRevision.builder()
                .observationId(obs.getId())
                .itemId(item.getId())
                .dimension(item.getDimension())
                .action("REVOKED")
                .reviewRound(dispute.getRoundNo())
                .previousFact(item.getCurrentFact())
                .previousGrade(item.getCurrentGrade())
                .previousSuggestion(item.getCurrentSuggestion())
                .reviewOpinion(request.getOpinion())
                .reviewerUsername(reviewer.getUsername())
                .reviewerName(reviewer.getRealName())
                .disputeId(dispute.getId())
                .build();
        revisionRepository.save(rev);

        item.setStatus("REVOKED");
        item.setLastReviewRound(dispute.getRoundNo());
        itemRepository.save(item);
    }

    // ============================ 分阶段改进目标 ============================

    @Override
    @Transactional
    public ObservationResponse createGoal(Long observationId, GoalCreateRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireSupervisorOrInstructor(obs, user);
        checkVersion(obs, request.getExpectedVersion());

        String status = obs.getStatus();
        // 双方可在改进中、延长后、证据被退回期间共同增补目标
        if (!Set.of(ST_IMPROVING, ST_EXTENDED, ST_EVIDENCE_PENDING_REVIEW, ST_REVIEW_OVERDUE).contains(status)) {
            throw new ObservationConflictException("仅改进阶段可以维护改进目标，当前状态：" + statusLabel(status));
        }
        int nextStage = (int) goalRepository.findByObservationIdOrderByStageNoAscIdAsc(obs.getId()).stream()
                .map(ImprovementGoal::getStageNo).max(Integer::compareTo).orElse(0) + 1;
        ImprovementGoal goal = ImprovementGoal.builder()
                .observationId(obs.getId())
                .stageNo(request.getStageNo() != null ? request.getStageNo() : nextStage)
                .title(request.getTitle())
                .description(request.getDescription())
                .criteria(request.getCriteria())
                .dueDate(request.getDueDate())
                .evidenceRequirement(request.getEvidenceRequirement())
                .requiredFlag(request.getRequiredFlag() == null || request.getRequiredFlag())
                .status("ACTIVE")
                .createdBy(user.getUsername())
                .createdByName(user.getRealName())
                .build();
        goal = goalRepository.save(goal);
        addEvent(obs.getId(), "GOAL_CREATED", user, status, status, "GOAL", goal.getId(),
                "形成第 " + goal.getStageNo() + " 阶段目标：" + goal.getTitle()
                        + "（衡量标准、截止时间、证明材料要求已明确）");
        return getDetail(obs.getId(), user);
    }

    // ============================ 证据回报与复查 ============================

    @Override
    @Transactional
    public ObservationResponse submitEvidence(Long observationId, Long goalId,
                                              EvidenceSubmitRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireInstructor(obs, user);
        checkVersion(obs, request.getExpectedVersion());

        String status = obs.getStatus();
        if (!Set.of(ST_IMPROVING, ST_EXTENDED, ST_EVIDENCE_PENDING_REVIEW, ST_REVIEW_OVERDUE, ST_RE_REVIEW)
                .contains(status)) {
            throw new ObservationConflictException("当前状态不能回报证据：" + statusLabel(status));
        }
        ImprovementGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("改进目标不存在: " + goalId));
        if (!goal.getObservationId().equals(obs.getId())) {
            throw new IllegalArgumentException("目标与观察单不匹配");
        }
        if ("ACHIEVED".equals(goal.getStatus())) {
            throw new ObservationConflictException("目标「" + goal.getTitle() + "」已取得有效证据，无需重复回报");
        }

        GoalEvidence evidence = GoalEvidence.builder()
                .goalId(goal.getId())
                .observationId(obs.getId())
                .status("SUBMITTED")
                .content(request.getContent())
                .attachmentNames(request.getAttachmentNames())
                .submittedBy(user.getUsername())
                .submittedByName(user.getRealName())
                .build();
        evidence = evidenceRepository.save(evidence);

        String from = obs.getStatus();
        obs.setStatus(ST_EVIDENCE_PENDING_REVIEW);
        observationRepository.save(obs);

        resolveReminders(obs.getId(), Set.of(RMD_GOAL_PROGRESS, RMD_INFO_REQUESTED, RMD_EVIDENCE_REJECTED),
                "讲师回报证据 #" + evidence.getId());
        addEvent(obs.getId(), "EVIDENCE_SUBMITTED", user, from, ST_EVIDENCE_PENDING_REVIEW,
                "EVIDENCE", evidence.getId(),
                "讲师就第 " + goal.getStageNo() + " 阶段目标回报证明材料，等待督导复查");
        remindObserver(obs, RMD_EVIDENCE_REVIEW, "evidence-" + evidence.getId(),
                "观察 #" + obs.getId() + " 目标「" + goal.getTitle() + "」有新证明材料待复查（48小时内）",
                LocalDateTime.now().plusHours(REVIEW_DUE_HOURS));
        return getDetail(obs.getId(), user);
    }

    @Override
    @Transactional
    public ObservationResponse reviewEvidence(Long observationId, Long evidenceId,
                                              EvidenceReviewRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireObserver(obs, user);
        checkVersion(obs, request.getExpectedVersion());

        String status = obs.getStatus();
        if (!Set.of(ST_EVIDENCE_PENDING_REVIEW, ST_REVIEW_OVERDUE).contains(status)) {
            throw new ObservationConflictException("当前没有待复查的证据，状态：" + statusLabel(status));
        }
        GoalEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new EntityNotFoundException("证据不存在: " + evidenceId));
        if (!evidence.getObservationId().equals(obs.getId())) {
            throw new IllegalArgumentException("证据与观察单不匹配");
        }
        if (!"SUBMITTED".equals(evidence.getStatus())) {
            throw new ObservationConflictException("该证据已复查（结论：" + evidence.getStatus()
                    + "），请勿重复提交复查意见");
        }

        String decision = request.getDecision().trim().toUpperCase();
        if (!Set.of("ACCEPTED", "REJECTED", "INFO_REQUESTED").contains(decision)) {
            throw new IllegalArgumentException("复查结论只能是 ACCEPTED / REJECTED / INFO_REQUESTED");
        }
        evidence.setStatus(decision);
        evidence.setReviewComment(request.getComment());
        evidence.setReviewedBy(user.getUsername());
        evidence.setReviewedByName(user.getRealName());
        evidence.setReviewedAt(LocalDateTime.now());
        evidenceRepository.save(evidence);

        ImprovementGoal goal = goalRepository.findById(evidence.getGoalId()).orElseThrow();
        resolveReminders(obs.getId(), Set.of(RMD_EVIDENCE_REVIEW, RMD_REVIEW_OVERDUE),
                "证据 #" + evidence.getId() + " 复查完成：" + decision);

        String from = obs.getStatus();
        String to;
        String eventDetail;
        switch (decision) {
            case "ACCEPTED" -> {
                goal.setStatus("ACHIEVED");
                goal.setAcceptedEvidenceId(evidence.getId());
                goal.setAchievedAt(LocalDateTime.now());
                goalRepository.save(goal);
                eventDetail = "证据有效，第 " + goal.getStageNo() + " 阶段目标达成";
                addEvent(obs.getId(), "EVIDENCE_REVIEWED", user, from, from, "EVIDENCE", evidence.getId(),
                        eventDetail + "：" + goal.getTitle());
                if (allRequiredGoalsAchieved(obs.getId())) {
                    obs.setStatus(ST_PENDING_OWNER_CLOSE);
                    to = ST_PENDING_OWNER_CLOSE;
                    observationRepository.save(obs);
                    addEvent(obs.getId(), "STATUS_CHANGED", user, from, to, null, null,
                            "全部必需目标均已有有效证据，转课程负责人结案");
                    remindOwnerClose(obs, "全部必需目标已取得有效证据，请课程负责人审核结案");
                } else {
                    // 仍有必需目标未达成，回到改进中继续推进
                    obs.setStatus(ST_IMPROVING);
                    to = ST_IMPROVING;
                    observationRepository.save(obs);
                    remindInstructorProgress(obs, "部分目标已达成，请继续回报其余必需目标的证据");
                }
            }
            case "REJECTED" -> {
                to = ST_IMPROVING;
                obs.setStatus(to);
                observationRepository.save(obs);
                eventDetail = "证明不充分，退回讲师重新准备";
                addEvent(obs.getId(), "EVIDENCE_REVIEWED", user, from, to, "EVIDENCE", evidence.getId(),
                        eventDetail + "（目标：" + goal.getTitle() + "）：" + request.getComment());
                remindInstructor(obs, RMD_EVIDENCE_REJECTED, "evidence-rejected-" + evidence.getId(),
                        "目标「" + goal.getTitle() + "」的证明被认定不充分，请按意见重新准备并再次回报",
                        goal.getDueDate());
            }
            default -> {
                to = ST_IMPROVING;
                obs.setStatus(to);
                observationRepository.save(obs);
                eventDetail = "要求讲师补充材料";
                addEvent(obs.getId(), "EVIDENCE_REVIEWED", user, from, to, "EVIDENCE", evidence.getId(),
                        eventDetail + "（目标：" + goal.getTitle() + "）：" + request.getComment());
                remindInstructor(obs, RMD_INFO_REQUESTED, "info-" + evidence.getId(),
                        "督导就目标「" + goal.getTitle() + "」的证据要求补充：" + request.getComment(),
                        goal.getDueDate());
            }
        }
        return getDetail(obs.getId(), user);
    }

    @Override
    @Transactional
    public ObservationResponse supplementEvidence(Long observationId, Long evidenceId,
                                                  EvidenceSupplementRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireInstructor(obs, user);
        checkVersion(obs, request.getExpectedVersion());
        GoalEvidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new EntityNotFoundException("证据不存在: " + evidenceId));
        if (!evidence.getObservationId().equals(obs.getId())) {
            throw new IllegalArgumentException("证据与观察单不匹配");
        }
        if (!"INFO_REQUESTED".equals(evidence.getStatus())) {
            throw new ObservationConflictException("仅“要求补充”的证据可以追加补充，当前状态：" + evidence.getStatus());
        }
        if (evidence.getSupplement() != null) {
            throw new ObservationConflictException("该证据已补充过；如需更多材料，请重新回报一条证据");
        }
        evidence.setSupplement(request.getSupplement());
        evidence.setSupplementedAt(LocalDateTime.now());
        // 补充后重新进入待复查，原回报内容原样保留
        evidence.setStatus("SUBMITTED");
        evidenceRepository.save(evidence);

        String from = obs.getStatus();
        obs.setStatus(ST_EVIDENCE_PENDING_REVIEW);
        observationRepository.save(obs);
        resolveReminders(obs.getId(), Set.of(RMD_INFO_REQUESTED), "讲师已补充材料");
        addEvent(obs.getId(), "EVIDENCE_SUPPLEMENTED", user, from, ST_EVIDENCE_PENDING_REVIEW,
                "EVIDENCE", evidence.getId(), "讲师按督导要求追加补充说明，原回报内容保持不变，重新待复查");
        remindObserver(obs, RMD_EVIDENCE_REVIEW, "evidence-" + evidence.getId(),
                "证据 #" + evidence.getId() + " 已补充材料，请再次复查",
                LocalDateTime.now().plusHours(REVIEW_DUE_HOURS));
        return getDetail(obs.getId(), user);
    }

    // ============================ 负责人：结案 / 延长 / 升级 ============================

    @Override
    @Transactional
    public ObservationResponse decideCycle(Long observationId, CycleDecisionRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireCourseAdmin(user);
        checkVersion(obs, request.getExpectedVersion());

        String decision = request.getDecision().trim().toUpperCase();
        String status = obs.getStatus();
        switch (decision) {
            case "CLOSE" -> doClose(obs, request, user, false);
            case "EXTEND" -> {
                if (request.getReason() == null || request.getReason().isBlank()) {
                    throw new IllegalArgumentException("延长周期必须写明依据");
                }
                if (request.getExtendUntil() == null) {
                    throw new IllegalArgumentException("延长周期必须填写新的期限");
                }
                if (!Set.of(ST_IMPROVING, ST_EXTENDED, ST_EVIDENCE_PENDING_REVIEW, ST_REVIEW_OVERDUE,
                        ST_PENDING_OWNER_CLOSE).contains(status)) {
                    throw new ObservationConflictException("当前状态不允许延长周期：" + statusLabel(status));
                }
                obs.setExtendUntil(request.getExtendUntil());
                obs.setExtensionCount(obs.getExtensionCount() + 1);
                String from = obs.getStatus();
                obs.setStatus(ST_EXTENDED);
                observationRepository.save(obs);
                resolveReminders(obs.getId(), Set.of(RMD_OWNER_CLOSE), "负责人决定延长周期");
                addEvent(obs.getId(), "CYCLE_EXTENDED", user, from, ST_EXTENDED, null, null,
                        "基于持续风险延长周期至 " + request.getExtendUntil() + "，依据：" + request.getReason());
                remindInstructorProgress(obs, "周期已延长至 " + request.getExtendUntil() + "，请继续回报进展");
                operationLogService.log("OBSERVATION_EXTEND", "TEACHING_OBSERVATION", obs.getId(),
                        "延长观察 #" + obs.getId() + " 改进周期，依据：" + request.getReason());
            }
            case "ESCALATE" -> {
                if (request.getReason() == null || request.getReason().isBlank()) {
                    throw new IllegalArgumentException("升级强化辅导必须写明依据");
                }
                if (!Set.of(ST_IMPROVING, ST_EXTENDED, ST_EVIDENCE_PENDING_REVIEW, ST_REVIEW_OVERDUE,
                        ST_PENDING_OWNER_CLOSE).contains(status)) {
                    throw new ObservationConflictException("当前状态不允许升级：" + statusLabel(status));
                }
                String from = obs.getStatus();
                obs.setStatus(ST_ESCALATED);
                obs.setEscalateReason(request.getReason());
                observationRepository.save(obs);
                resolveReminders(obs.getId(),
                        Set.of(RMD_OWNER_CLOSE, RMD_GOAL_PROGRESS, RMD_EVIDENCE_REVIEW, RMD_REVIEW_OVERDUE,
                                RMD_EVIDENCE_REJECTED, RMD_INFO_REQUESTED),
                        "负责人升级为强化辅导");
                addEvent(obs.getId(), "CYCLE_ESCALATED", user, from, ST_ESCALATED, null, null,
                        "基于持续风险升级为强化辅导，依据：" + request.getReason());
                remindSupervisors(obs, RMD_REVIEW_OVERDUE, "escalation-" + obs.getId(),
                        "观察 #" + obs.getId() + " 已升级为强化辅导，请安排专项带教");
                operationLogService.log("OBSERVATION_ESCALATE", "TEACHING_OBSERVATION", obs.getId(),
                        "观察 #" + obs.getId() + " 升级强化辅导，依据：" + request.getReason());
            }
            default -> throw new IllegalArgumentException("决策类型只能是 CLOSE / EXTEND / ESCALATE");
        }
        return getDetail(obs.getId(), user);
    }

    private void doClose(TeachingObservation obs, CycleDecisionRequest request, AuthUser user, boolean afterRereview) {
        String status = obs.getStatus();
        boolean noNeedGate = ST_NO_IMPROVEMENT_NEEDED.equals(status);
        boolean goalsGate = ST_PENDING_OWNER_CLOSE.equals(status);
        if (!noNeedGate && !goalsGate) {
            throw new ObservationConflictException("仅“待负责人结案”状态可以结案，当前状态：" + statusLabel(status)
                    + "；必需目标未全部取得有效证据前不得结案");
        }
        if ((request.getPublicSummary() == null || request.getPublicSummary().isBlank())) {
            throw new IllegalArgumentException("结案必须填写面向查看者的最终结论");
        }
        // 结案闸门：必需目标全部要有有效证据（无需改进路径除外）
        if (!noNeedGate && !allRequiredGoalsAchieved(obs.getId())) {
            throw new ObservationConflictException("仍有必需目标缺少有效证据，不能结案");
        }
        // 幂等：重复点击结案 / 网络重试 —— 当前已存在指向本结论的结案状态时明确 409
        if (ST_CLOSED.equals(status)) {
            throw new ObservationConflictException("该周期已结案，请勿重复点击");
        }

        String resultType = request.getResultType() != null ? request.getResultType()
                : (noNeedGate ? "NO_IMPROVEMENT_NEEDED" : "IMPROVED");
        if (!Set.of("IMPROVED", "NO_IMPROVEMENT_NEEDED").contains(resultType)) {
            throw new IllegalArgumentException("结案结果只能是 IMPROVED / NO_IMPROVEMENT_NEEDED");
        }

        int versionNo = (int) conclusionRepository.countByObservationId(obs.getId()) + 1;
        Long supersedes = obs.getCurrentConclusionId();
        CycleConclusion conclusion = CycleConclusion.builder()
                .observationId(obs.getId())
                .versionNo(versionNo)
                .status("CLOSED")
                .resultType(resultType)
                .publicSummary(request.getPublicSummary())
                .internalNote(request.getInternalNote())
                .decidedBy(user.getUsername())
                .decidedByName(user.getRealName())
                .supersedesConclusionId(supersedes)
                .build();
        conclusion = conclusionRepository.save(conclusion);

        // 旧结论转为历史版本，新结论说明替代关系（不覆盖）
        if (supersedes != null) {
            conclusionRepository.findById(supersedes).ifPresent(old -> {
                old.setStatus("SUPERSEDED");
                old.setSupersededByConclusionId(conclusion.getId());
                conclusionRepository.save(old);
            });
            addEvent(obs.getId(), "CYCLE_CLOSED", user, status, ST_CLOSED, "CONCLUSION", conclusion.getId(),
                    "重开复查后形成第 " + versionNo + " 版结论，替代 v" + (versionNo - 1)
                            + "；旧结论作为历史版本保留，新结论：" + request.getPublicSummary());
        } else {
            addEvent(obs.getId(), "CYCLE_CLOSED", user, status, ST_CLOSED, "CONCLUSION", conclusion.getId(),
                    "周期结案（v" + versionNo + "）：" + request.getPublicSummary());
        }

        obs.setStatus(ST_CLOSED);
        obs.setCurrentConclusionId(conclusion.getId());
        obs.setClosedAt(LocalDateTime.now());
        if (afterRereview) {
            // 重开路径恢复结案
        }
        observationRepository.save(obs);

        resolveReminders(obs.getId(),
                Set.of(RMD_OWNER_CLOSE, RMD_GOAL_PROGRESS, RMD_EVIDENCE_REVIEW, RMD_REVIEW_OVERDUE,
                        RMD_EVIDENCE_REJECTED, RMD_INFO_REQUESTED, RMD_RE_REVIEW),
                "周期已结案（结论 v" + versionNo + "）");
        operationLogService.log("OBSERVATION_CLOSE", "TEACHING_OBSERVATION", obs.getId(),
                "观察 #" + obs.getId() + " 结案 v" + versionNo);
    }

    // ============================ 重开（结案后证据错误） ============================

    @Override
    @Transactional
    public ObservationResponse requestReopen(Long observationId, ReopenRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        // 督导或讲师本人可以发起重开申请
        if (!user.hasRole(ROLE_SUPERVISOR) && !user.hasRole(ROLE_COURSE_ADMIN)
                && !isBoundInstructor(obs, user)) {
            throw new ForbiddenException("仅督导、课程负责人或讲师本人可以申请重开");
        }
        checkVersion(obs, request.getExpectedVersion());

        if (!ST_CLOSED.equals(obs.getStatus())) {
            throw new ObservationConflictException("只有已结案的周期可以申请重开，当前状态："
                    + statusLabel(obs.getStatus()));
        }
        Optional<ObservationReopenRequest> pending =
                reopenRepository.findFirstByObservationIdAndStatusOrderByIdDesc(obs.getId(), "PENDING");
        if (pending.isPresent()) {
            throw new ObservationConflictException("已有待审批的重开申请 #" + pending.get().getId()
                    + "，请勿重复提交");
        }

        ObservationReopenRequest reopen = ObservationReopenRequest.builder()
                .observationId(obs.getId())
                .reason(request.getReason())
                .requestedBy(user.getUsername())
                .requestedByName(user.getRealName())
                .status("PENDING")
                .build();
        reopen = reopenRepository.save(reopen);

        String from = obs.getStatus();
        obs.setStatus(ST_REOPEN_REQUESTED);
        observationRepository.save(obs);
        addEvent(obs.getId(), "REOPEN_REQUESTED", user, from, ST_REOPEN_REQUESTED, "REOPEN", reopen.getId(),
                "结案后发现证据错误，申请重开：" + request.getReason() + "；原结论将保留为历史版本");
        remindOwner(obs, RMD_REOPEN_APPROVAL, "reopen-" + reopen.getId(),
                "观察 #" + obs.getId() + " 申请重开（声称证据错误），请审批；批准后恢复待复查，原结论保留");
        operationLogService.log("OBSERVATION_REOPEN_REQUEST", "TEACHING_OBSERVATION", obs.getId(),
                "观察 #" + obs.getId() + " 申请重开：" + request.getReason());
        return getDetail(obs.getId(), user);
    }

    @Override
    @Transactional
    public ObservationResponse decideReopen(Long observationId, Long reopenId,
                                            ReopenDecisionRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireCourseAdmin(user);
        checkVersion(obs, request.getExpectedVersion());
        if (!ST_REOPEN_REQUESTED.equals(obs.getStatus())) {
            throw new ObservationConflictException("当前没有待审批的重开申请，状态：" + statusLabel(obs.getStatus()));
        }
        ObservationReopenRequest reopen = reopenRepository.findById(reopenId)
                .orElseThrow(() -> new EntityNotFoundException("重开申请不存在: " + reopenId));
        if (!reopen.getObservationId().equals(obs.getId()) || !"PENDING".equals(reopen.getStatus())) {
            throw new ObservationConflictException("重开申请已处理或与观察单不匹配，请勿重复审批");
        }
        String decision = request.getDecision().trim().toUpperCase();
        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new IllegalArgumentException("审批结论只能是 APPROVED / REJECTED");
        }

        reopen.setStatus(decision);
        reopen.setDecisionComment(request.getComment());
        reopen.setDecidedBy(user.getUsername());
        reopen.setDecidedByName(user.getRealName());
        reopen.setDecidedAt(LocalDateTime.now());
        reopenRepository.save(reopen);

        String from = obs.getStatus();
        if ("APPROVED".equals(decision)) {
            obs.setStatus(ST_RE_REVIEW);
            obs.setReopenedFromConclusionId(obs.getCurrentConclusionId());
            observationRepository.save(obs);
            resolveReminders(obs.getId(), Set.of(RMD_REOPEN_APPROVAL), "重开申请已批准");
            addEvent(obs.getId(), "REOPEN_DECIDED", user, from, ST_RE_REVIEW, "REOPEN", reopen.getId(),
                    "批准重开，周期恢复到待复查；原结论 v" + conclusionVersionNo(obs.getCurrentConclusionId())
                            + " 继续作为历史版本保留：" + request.getComment());
            remindObserver(obs, RMD_RE_REVIEW, "rereview-" + reopen.getId(),
                    "观察 #" + obs.getId() + " 已批准重开，请对存疑证据重新复查", null);
        } else {
            obs.setStatus(ST_CLOSED);
            observationRepository.save(obs);
            resolveReminders(obs.getId(), Set.of(RMD_REOPEN_APPROVAL), "重开申请未批准");
            addEvent(obs.getId(), "REOPEN_DECIDED", user, from, ST_CLOSED, "REOPEN", reopen.getId(),
                    "未批准重开，维持原结案结论：" + request.getComment());
        }
        operationLogService.log("OBSERVATION_REOPEN_DECIDE", "TEACHING_OBSERVATION", obs.getId(),
                "观察 #" + obs.getId() + " 重开审批：" + decision);
        return getDetail(obs.getId(), user);
    }

    @Override
    @Transactional
    public ObservationResponse submitRereview(Long observationId, RereviewRequest request, AuthUser user) {
        TeachingObservation obs = lock(observationId);
        requireObserver(obs, user);
        checkVersion(obs, request.getExpectedVersion());
        if (!ST_RE_REVIEW.equals(obs.getStatus())) {
            throw new ObservationConflictException("当前不在重开待复查状态：" + statusLabel(obs.getStatus()));
        }
        String from = obs.getStatus();
        resolveReminders(obs.getId(), Set.of(RMD_RE_REVIEW), "督导完成重开复查");
        if (Boolean.TRUE.equals(request.getImprovementStillNeeded())) {
            enterImproving(obs, from, user, "重开复查认为仍需改进，恢复改进阶段：" + request.getComment());
        } else {
            obs.setStatus(ST_PENDING_OWNER_CLOSE);
            observationRepository.save(obs);
            addEvent(obs.getId(), "RE_REVIEWED", user, from, ST_PENDING_OWNER_CLOSE, null, null,
                    "重开复查完成，认为无需继续改进，转负责人重新结案：" + request.getComment());
            remindOwnerClose(obs, "重开复查完成，请课程负责人基于复查结果形成新版结论");
        }
        return getDetail(obs.getId(), user);
    }

    // ============================ 提醒与逾期 ============================

    @Override
    @Transactional(readOnly = true)
    public List<ObservationReminderResponse> myReminders(AuthUser user) {
        List<ObservationReminder> rows;
        if (user.hasRole(ROLE_ADMIN)) {
            rows = reminderRepository.findByStatusOrderByCreatedAtDesc("OPEN");
        } else if (user.hasRole(ROLE_COURSE_ADMIN)) {
            rows = reminderRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                    .filter(r -> user.getUsername().equals(r.getAssigneeUsername())
                            || Set.of(RMD_OWNER_CLOSE, RMD_REOPEN_APPROVAL).contains(r.getReminderType()))
                    .toList();
        } else if (user.hasRole(ROLE_SUPERVISOR)) {
            // 督导：本人负责的 + 广播给全体督导的（DISPUTE_REVIEW 等）
            rows = reminderRepository.findByStatusOrderByCreatedAtDesc("OPEN").stream()
                    .filter(r -> user.getUsername().equals(r.getAssigneeUsername())
                            || isSupervisorBroadcast(r.getReminderType()))
                    .toList();
        } else {
            rows = reminderRepository.findByAssigneeUsernameAndStatusOrderByCreatedAtDesc(
                    user.getUsername(), "OPEN");
        }
        return rows.stream().map(this::toReminderResponse).toList();
    }

    @Override
    @Transactional
    public int scanOverdue() {
        int affected = 0;
        LocalDateTime now = LocalDateTime.now();

        for (TeachingObservation obs : observationRepository.findAckOverdue(now)) {
            TeachingObservation locked = lock(obs.getId());
            if (ST_PENDING_ACK.equals(locked.getStatus())) {
                String from = locked.getStatus();
                locked.setStatus(ST_RESPONSE_OVERDUE);
                observationRepository.save(locked);
                resolveReminders(locked.getId(), Set.of(RMD_ACK_DUE), "确认/回应已逾期");
                addEvent(locked.getId(), "OVERDUE_MARKED", null, from, ST_RESPONSE_OVERDUE, null, null,
                        "讲师超过 " + locked.getAckDueAt() + " 未确认/回应，标记回应逾期");
                User instructorUser = instructorUser(locked.getInstructorId());
                remind(locked, RMD_RESPONSE_OVERDUE, "response-overdue-" + locked.getAckRound(),
                        instructorUser, locked.getInstructorName(),
                        "你已超过期限未确认带教观察 #" + locked.getId() + "，请尽快确认或提出异议",
                        locked.getAckDueAt());
                remindObserver(locked, RMD_RESPONSE_OVERDUE, "observer-response-overdue-" + locked.getAckRound(),
                        "讲师对观察 #" + locked.getId() + " 逾期未确认，请跟进", locked.getAckDueAt());
                affected++;
            }
        }

        for (TeachingObservation obs : observationRepository.findEvidencePendingReview()) {
            TeachingObservation locked = lock(obs.getId());
            if (!ST_EVIDENCE_PENDING_REVIEW.equals(locked.getStatus())) {
                continue;
            }
            // 最早一条未复查证据的提交时间
            LocalDateTime oldest = evidenceRepository.findByObservationIdOrderByIdAsc(locked.getId()).stream()
                    .filter(e -> "SUBMITTED".equals(e.getStatus()))
                    .map(GoalEvidence::getSubmittedAt)
                    .min(LocalDateTime::compareTo).orElse(null);
            if (oldest != null && oldest.plusHours(REVIEW_DUE_HOURS).isBefore(now)) {
                String from = locked.getStatus();
                locked.setStatus(ST_REVIEW_OVERDUE);
                observationRepository.save(locked);
                resolveReminders(locked.getId(), Set.of(RMD_EVIDENCE_REVIEW), "督导复查已逾期");
                addEvent(locked.getId(), "OVERDUE_MARKED", null, from, ST_REVIEW_OVERDUE, null, null,
                        "督导超过 " + REVIEW_DUE_HOURS + " 小时未复查证据，标记复查逾期");
                String dedup = "review-overdue-"
                        + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH").format(oldest);
                remindObserver(locked, RMD_REVIEW_OVERDUE, dedup,
                        "观察 #" + locked.getId() + " 的证据复查已超过 " + REVIEW_DUE_HOURS + " 小时，请尽快复查",
                        oldest.plusHours(REVIEW_DUE_HOURS));
                affected++;
            }
        }
        return affected;
    }

    // ============================ 查询与分级投影 ============================

    @Override
    @Transactional(readOnly = true)
    public ObservationResponse getDetail(Long observationId, AuthUser user) {
        TeachingObservation obs = observationRepository.findById(observationId)
                .orElseThrow(() -> new EntityNotFoundException("观察不存在: " + observationId));
        String view = resolveViewLevel(obs, user);
        return toResponse(obs, view, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationListItemResponse> list(Long courseId, Long instructorId, String status, AuthUser user) {
        boolean supervisorLike = user.hasRole(ROLE_SUPERVISOR) || user.hasRole(ROLE_COURSE_ADMIN)
                || user.hasRole(ROLE_ADMIN);

        List<TeachingObservation> all;
        if (courseId != null) {
            all = observationRepository.findByCourseIdOrderByObservedAtDesc(courseId);
        } else if (instructorId != null) {
            all = observationRepository.findByInstructorIdOrderByObservedAtDesc(instructorId);
        } else {
            all = observationRepository.findAllByOrderByObservedAtDesc();
        }

        List<ObservationListItemResponse> result = new ArrayList<>();
        for (TeachingObservation obs : all) {
            if (status != null && !status.isBlank() && !status.equals(obs.getStatus())) {
                continue;
            }
            String view = resolveViewLevel(obs, user);
            // 讲师只能看到本人观察
            if ("INSTRUCTOR".equals(view) && !isBoundInstructor(obs, user)) {
                continue;
            }
            List<ImprovementGoal> goals = goalRepository
                    .findByObservationIdOrderByStageNoAscIdAsc(obs.getId());
            int required = (int) goals.stream().filter(g -> Boolean.TRUE.equals(g.getRequiredFlag())).count();
            int achieved = (int) goals.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getRequiredFlag()) && "ACHIEVED".equals(g.getStatus()))
                    .count();
            String summary = obs.getCurrentConclusionId() == null ? null
                    : conclusionRepository.findById(obs.getCurrentConclusionId())
                    .map(CycleConclusion::getPublicSummary).orElse(null);
            Integer versionNo = obs.getCurrentConclusionId() == null ? null
                    : conclusionRepository.findById(obs.getCurrentConclusionId())
                    .map(CycleConclusion::getVersionNo).orElse(null);

            List<String> myTypes = myOpenReminderTypes(obs.getId(), user);

            result.add(ObservationListItemResponse.builder()
                    .id(obs.getId())
                    .courseId(obs.getCourseId())
                    .courseName(obs.getCourseName())
                    .sessionNo(obs.getSessionNo())
                    .instructorId(obs.getInstructorId())
                    .instructorName(obs.getInstructorName())
                    .observedAt(obs.getObservedAt())
                    .observerName(supervisorLike ? obs.getObserverName() : null)
                    .title(view.equals("PUBLIC") ? "带教观察记录" : obs.getTitle())
                    .status(obs.getStatus())
                    .statusLabel(statusLabel(obs.getStatus()))
                    .viewLevel(view)
                    .version(obs.getObjVersion())
                    .requiredGoalCount(required)
                    .achievedRequiredGoalCount(achieved)
                    .allRequiredGoalsAchieved(required > 0 && required == achieved)
                    .currentConclusionSummary(summary)
                    .conclusionVersionNo(versionNo)
                    .myPendingReminderTypes(myTypes)
                    .createdAt(obs.getCreatedAt())
                    .build());
        }
        return result;
    }

    // ---------------- 投影 ----------------

    private ObservationResponse toResponse(TeachingObservation obs, String view, AuthUser user) {
        List<ObservationItem> items = itemRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ObservationAddendum> addenda = addendumRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ObservationDispute> disputes = disputeRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ImprovementGoal> goals = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(obs.getId());
        List<ObservationAttachment> attachments = attachmentRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<CycleConclusion> conclusions = conclusionRepository.findByObservationIdOrderByVersionNoAsc(obs.getId());
        List<ObservationReopenRequest> reopens = reopenRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ObservationEvent> events = eventRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ObservationReminder> reminders = reminderRepository.findByObservationIdOrderByIdAsc(obs.getId());

        ObservationResponse.ObservationResponseBuilder b = ObservationResponse.builder()
                .id(obs.getId())
                .courseId(obs.getCourseId())
                .instructorId(obs.getInstructorId())
                .status(obs.getStatus())
                .statusLabel(statusLabel(obs.getStatus()))
                .viewLevel(view)
                .version(obs.getObjVersion())
                .disputeRoundsResolved(obs.getDisputeRoundsResolved())
                .reopenedFromConclusionId(obs.getReopenedFromConclusionId())
                .createdAt(obs.getCreatedAt())
                .updatedAt(obs.getUpdatedAt());

        if ("PUBLIC".equals(view)) {
            // 普通查看者：周期状态、进度、最终结论，其他全部不下发
            return b
                    .courseName(obs.getCourseName())
                    .sessionNo(obs.getSessionNo())
                    .instructorName(obs.getInstructorName())
                    .observedAt(obs.getObservedAt())
                    .title("带教观察记录")
                    .conclusions(conclusions.stream().map(c -> ObservationResponse.ConclusionView.builder()
                            .id(c.getId())
                            .versionNo(c.getVersionNo())
                            .status(c.getStatus())
                            .resultType(c.getResultType())
                            .publicSummary(c.getPublicSummary())
                            .decidedAt(c.getDecidedAt())
                            .supersedesConclusionId(c.getSupersedesConclusionId())
                            .supersededByConclusionId(c.getSupersededByConclusionId())
                            .build()).toList())
                    .events(events.stream()
                            .filter(e -> PUBLIC_EVENTS.contains(e.getEventType()))
                            .map(e -> toEventView(e, false)).toList())
                    .openReminders(List.of())
                    .build();
        }

        // FULL 与 INSTRUCTOR 共同可见的基础信息
        b.courseName(obs.getCourseName())
                .sessionNo(obs.getSessionNo())
                .instructorName(obs.getInstructorName())
                .observedAt(obs.getObservedAt())
                .observerUsername(obs.getObserverUsername())
                .observerName(obs.getObserverName())
                .title(obs.getTitle())
                .attachmentSource(obs.getAttachmentSource())
                .ackDueAt(obs.getAckDueAt())
                .acknowledgedAt(obs.getAcknowledgedAt())
                .acknowledgedBy(obs.getAcknowledgedBy())
                .instructorResponse(obs.getInstructorResponse())
                .respondedAt(obs.getRespondedAt())
                .improvingSince(obs.getImprovingSince())
                .closedAt(obs.getClosedAt())
                .ownerName(obs.getOwnerName());

        boolean full = "FULL".equals(view);
        if (full) {
            b.summary(obs.getSummary())
                    .ownerUsername(obs.getOwnerUsername())
                    .rejectReason(obs.getRejectReason())
                    .ackRound(obs.getAckRound())
                    .improveRound(obs.getImproveRound())
                    .extendUntil(obs.getExtendUntil())
                    .extensionCount(obs.getExtensionCount())
                    .escalateReason(obs.getEscalateReason());
        }

        // 观察项分级投影
        b.items(items.stream().map(i -> toItemView(i, full)).toList());
        if (full) {
            b.itemRevisions(revisionRepository.findByObservationIdOrderByIdAsc(obs.getId()).stream()
                    .map(this::toRevisionView).toList());
        } else {
            // 讲师也需要知道自己提的异议引发了哪些“可感知”的修订动作（不看别人的内部处理原文）
            b.itemRevisions(revisionRepository.findByObservationIdOrderByIdAsc(obs.getId()).stream()
                    .filter(r -> disputes.stream().anyMatch(d -> d.getId().equals(r.getDisputeId())
                            && user.getUsername().equals(d.getRaisedBy())))
                    .map(r -> ObservationResponse.ItemRevisionView.builder()
                            .id(r.getId())
                            .itemId(r.getItemId())
                            .dimension(r.getDimension())
                            .action(r.getAction())
                            .reviewRound(r.getReviewRound())
                            .reviewerName(r.getReviewerName())
                            .disputeId(r.getDisputeId())
                            .createdAt(r.getCreatedAt())
                            .newGrade(r.getNewGrade())
                            .build())
                    .toList());
        }

        b.addenda(addenda.stream()
                .filter(a -> full || LV_IMPROVEMENT.equals(a.getSensitivityLevel()) || LV_PUBLIC.equals(a.getSensitivityLevel()))
                .map(a -> ObservationResponse.AddendumView.builder()
                        .id(a.getId())
                        .content(a.getContent())
                        .sensitivityLevel(a.getSensitivityLevel())
                        .createdBy(full ? a.getCreatedBy() : null)
                        .createdByName(a.getCreatedByName())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList());

        b.attachments(attachments.stream()
                .filter(a -> full || LV_IMPROVEMENT.equals(a.getSensitivityLevel()) || LV_PUBLIC.equals(a.getSensitivityLevel()))
                .map(a -> ObservationResponse.AttachmentView.builder()
                        .id(a.getId())
                        .fileName(a.getFileName())
                        .fileRef(a.getFileRef())
                        .sourceNote(a.getSourceNote())
                        .sensitivityLevel(a.getSensitivityLevel())
                        .uploadedBy(full ? a.getUploadedBy() : null)
                        .uploadedByName(a.getUploadedByName())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList());

        b.disputes(disputes.stream().map(d -> {
            var db = ObservationResponse.DisputeView.builder()
                    .id(d.getId())
                    .roundNo(d.getRoundNo())
                    .reason(d.getReason())
                    .raisedBy(d.getRaisedBy())
                    .raisedByName(d.getRaisedByName())
                    .raisedAt(d.getRaisedAt())
                    .status(d.getStatus())
                    .resolution(d.getResolution())
                    .resolvedAt(d.getResolvedAt());
            if (full) {
                db.itemIds(parseItemIds(d.getItemIds()))
                        .resolutionOpinion(d.getResolutionOpinion())
                        .reviewerUsername(d.getReviewerUsername())
                        .reviewerName(d.getReviewerName());
            } else if (user.getUsername().equals(d.getRaisedBy())) {
                // 讲师本人：自己的异议可见复核结论意见
                db.itemIds(parseItemIds(d.getItemIds()))
                        .resolutionOpinion(d.getResolutionOpinion())
                        .reviewerName(d.getReviewerName());
            }
            return db.build();
        }).toList());

        b.goals(goals.stream().map(g -> {
            List<GoalEvidence> evs = evidenceRepository.findByGoalIdOrderByIdAsc(g.getId());
            return ObservationResponse.GoalView.builder()
                    .id(g.getId())
                    .stageNo(g.getStageNo())
                    .title(g.getTitle())
                    .description(g.getDescription())
                    .criteria(g.getCriteria())
                    .dueDate(g.getDueDate())
                    .evidenceRequirement(g.getEvidenceRequirement())
                    .requiredFlag(g.getRequiredFlag())
                    .status(g.getStatus())
                    .createdBy(full ? g.getCreatedBy() : null)
                    .createdByName(g.getCreatedByName())
                    .achievedAt(g.getAchievedAt())
                    .acceptedEvidenceId(g.getAcceptedEvidenceId())
                    .evidences(evs.stream().map(e -> ObservationResponse.EvidenceView.builder()
                            .id(e.getId())
                            .status(e.getStatus())
                            .content(e.getContent())
                            .attachmentNames(e.getAttachmentNames())
                            .submittedBy(full ? e.getSubmittedBy() : null)
                            .submittedByName(e.getSubmittedByName())
                            .submittedAt(e.getSubmittedAt())
                            .reviewComment(e.getReviewComment())
                            .reviewedBy(full ? e.getReviewedBy() : null)
                            .reviewedByName(e.getReviewedByName())
                            .reviewedAt(e.getReviewedAt())
                            .supplement(e.getSupplement())
                            .supplementedAt(e.getSupplementedAt())
                            .build()).toList())
                    .build();
        }).toList());

        b.conclusions(conclusions.stream().map(c -> ObservationResponse.ConclusionView.builder()
                .id(c.getId())
                .versionNo(c.getVersionNo())
                .status(c.getStatus())
                .resultType(c.getResultType())
                .publicSummary(c.getPublicSummary())
                .internalNote(full ? c.getInternalNote() : null)
                .decidedBy(full ? c.getDecidedBy() : null)
                .decidedByName(c.getDecidedByName())
                .decidedAt(c.getDecidedAt())
                .supersedesConclusionId(c.getSupersedesConclusionId())
                .supersededByConclusionId(c.getSupersededByConclusionId())
                .build()).toList());

        b.reopenRequests(full ? reopens.stream().map(r -> ObservationResponse.ReopenView.builder()
                .id(r.getId())
                .reason(r.getReason())
                .requestedBy(r.getRequestedBy())
                .requestedByName(r.getRequestedByName())
                .requestedAt(r.getRequestedAt())
                .status(r.getStatus())
                .decisionComment(r.getDecisionComment())
                .decidedBy(r.getDecidedBy())
                .decidedByName(r.getDecidedByName())
                .decidedAt(r.getDecidedAt())
                .build()).toList()
                : reopens.stream()
                .filter(r -> user.getUsername().equals(r.getRequestedBy()))
                .map(r -> ObservationResponse.ReopenView.builder()
                        .id(r.getId())
                        .reason(r.getReason())
                        .requestedByName(r.getRequestedByName())
                        .requestedAt(r.getRequestedAt())
                        .status(r.getStatus())
                        .decisionComment(r.getDecisionComment())
                        .decidedByName(r.getDecidedByName())
                        .decidedAt(r.getDecidedAt())
                        .build()).toList());

        b.events(events.stream().map(e -> toEventView(e, full)).toList());
        b.openReminders(reminders.stream().filter(r -> "OPEN".equals(r.getStatus()))
                .filter(r -> full
                        || user.getUsername().equals(r.getAssigneeUsername())
                        || (user.hasRole(ROLE_SUPERVISOR) && isSupervisorBroadcast(r.getReminderType()))
                        || (user.hasRole(ROLE_COURSE_ADMIN)
                            && Set.of(RMD_OWNER_CLOSE, RMD_REOPEN_APPROVAL).contains(r.getReminderType())))
                .map(this::toReminderInline).toList());

        return b.build();
    }

    private ObservationResponse.ItemView toItemView(ObservationItem i, boolean full) {
        ObservationResponse.ItemView.ItemViewBuilder b = ObservationResponse.ItemView.builder()
                .id(i.getId())
                .dimension(i.getDimension())
                .dimensionLabel(dimensionLabel(i.getDimension()))
                .factLevel(i.getFactLevel())
                .factLevelLabel(levelLabel(i.getFactLevel()))
                .grade(i.getCurrentGrade())
                .gradeInternal(i.getGradeInternal())
                .suggestion(i.getCurrentSuggestion())
                .suggestionLevel(i.getSuggestionLevel())
                .status(i.getStatus())
                .lastReviewRound(i.getLastReviewRound())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt());

        if (full) {
            b.fact(i.getCurrentFact())
                    .masked(false)
                    .originalFact(i.getOriginalFact())
                    .originalGrade(i.getOriginalGrade())
                    .originalSuggestion(i.getOriginalSuggestion());
        } else {
            // 讲师视图：INTERNAL 事实不可见；SENSITIVE 只下发脱敏文；其余下发当前事实
            switch (i.getFactLevel()) {
                case LV_INTERNAL -> b.fact("【内部判断，仅督导/负责人可见】").masked(true);
                case LV_SENSITIVE -> b.fact(i.getMaskedFact() != null ? i.getMaskedFact()
                        : maskingService.maskStudentIdentity(i.getCurrentFact())).masked(true);
                default -> b.fact(i.getCurrentFact()).masked(false);
            }
            if (Boolean.TRUE.equals(i.getGradeInternal())) {
                // 内部等级理由不展示，等级本身以是否达标的方式保留给讲师改进参考
                b.grade(null).gradeInternal(true);
            }
            // 撤销项不向讲师展示改进建议（该项已无效）
            if ("REVOKED".equals(i.getStatus())) {
                b.suggestion(null).fact("该项经异议复核已撤销");
            }
        }
        return b.build();
    }

    private ObservationResponse.ItemRevisionView toRevisionView(ObservationItemRevision r) {
        return ObservationResponse.ItemRevisionView.builder()
                .id(r.getId())
                .itemId(r.getItemId())
                .dimension(r.getDimension())
                .action(r.getAction())
                .reviewRound(r.getReviewRound())
                .previousFact(r.getPreviousFact())
                .previousGrade(r.getPreviousGrade())
                .previousSuggestion(r.getPreviousSuggestion())
                .newFact(r.getNewFact())
                .newGrade(r.getNewGrade())
                .newSuggestion(r.getNewSuggestion())
                .reviewOpinion(r.getReviewOpinion())
                .reviewerUsername(r.getReviewerUsername())
                .reviewerName(r.getReviewerName())
                .disputeId(r.getDisputeId())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ObservationResponse.EventView toEventView(ObservationEvent e, boolean full) {
        return ObservationResponse.EventView.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .eventTypeLabel(eventLabel(e.getEventType()))
                .actorUsername(full ? e.getActorUsername() : null)
                .actorName(e.getActorName())
                .fromStatus(full ? e.getFromStatus() : null)
                .toStatus(e.getToStatus())
                .refType(full ? e.getRefType() : null)
                .refId(full ? e.getRefId() : null)
                .detail(e.getDetail())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ObservationResponse.ReminderView toReminderInline(ObservationReminder r) {
        return ObservationResponse.ReminderView.builder()
                .id(r.getId())
                .reminderType(r.getReminderType())
                .reminderTypeLabel(reminderLabel(r.getReminderType()))
                .assigneeUsername(r.getAssigneeUsername())
                .assigneeName(r.getAssigneeName())
                .message(r.getMessage())
                .dueAt(r.getDueAt())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ObservationReminderResponse toReminderResponse(ObservationReminder r) {
        TeachingObservation obs = observationRepository.findById(r.getObservationId()).orElse(null);
        return ObservationReminderResponse.builder()
                .id(r.getId())
                .observationId(r.getObservationId())
                .observationTitle(obs != null ? obs.getTitle() : null)
                .courseName(obs != null ? obs.getCourseName() : null)
                .instructorName(obs != null ? obs.getInstructorName() : null)
                .reminderType(r.getReminderType())
                .reminderTypeLabel(reminderLabel(r.getReminderType()))
                .assigneeUsername(r.getAssigneeUsername())
                .assigneeName(r.getAssigneeName())
                .message(r.getMessage())
                .status(r.getStatus())
                .dueAt(r.getDueAt())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }

    // ============================ 内部辅助 ============================

    private static final Set<String> PUBLIC_EVENTS = Set.of(
            "CYCLE_CLOSED", "CYCLE_ESCALATED", "CYCLE_EXTENDED", "REOPEN_DECIDED", "STATUS_CHANGED");

    private TeachingObservation lock(Long id) {
        return observationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("观察不存在: " + id));
    }

    private void checkVersion(TeachingObservation obs, Long expected) {
        if (expected == null) {
            throw new IllegalArgumentException("缺少版本校验字段，请刷新页面后重试");
        }
        if (!expected.equals(obs.getObjVersion())) {
            throw new ObservationConflictException("页面内容已过期（当前版本 v" + obs.getObjVersion()
                    + "，你的页面基于 v" + expected + "）。该观察已被其他操作更新，"
                    + "请刷新获取最新复核结果后再操作，旧内容未被保存");
        }
    }

    private void enterImproving(TeachingObservation obs, String from, AuthUser user, String detail) {
        obs.setStatus(ST_IMPROVING);
        obs.setImprovingSince(LocalDateTime.now());
        obs.setImproveRound(obs.getImproveRound() + 1);
        observationRepository.save(obs);
        addEvent(obs.getId(), "STATUS_CHANGED", user, from, ST_IMPROVING, null, null, detail);
        remindInstructorProgress(obs, "请与督导共同形成分阶段改进目标，并按衡量标准回报证明材料");
    }

    private List<ObservationItem> activeItems(Long obsId) {
        return itemRepository.findByObservationIdOrderByIdAsc(obsId).stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()) || "MODIFIED".equals(i.getStatus()))
                .toList();
    }

    private boolean allRequiredGoalsAchieved(Long obsId) {
        List<ImprovementGoal> goals = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(obsId);
        List<ImprovementGoal> required = goals.stream().filter(g -> Boolean.TRUE.equals(g.getRequiredFlag())).toList();
        // 无需改进路径（没有目标）由调用方走 NO_IMPROVEMENT_NEEDED；进入结案闸门时至少要有一个必需目标
        if (required.isEmpty()) {
            return false;
        }
        return required.stream().allMatch(g -> "ACHIEVED".equals(g.getStatus()));
    }

    private String normalizeGrade(String grade) {
        if (grade == null) {
            throw new IllegalArgumentException("等级不能为空");
        }
        String g = grade.trim().toUpperCase();
        if (!Set.of("A", "B", "C", "D").contains(g)) {
            throw new IllegalArgumentException("等级只能是 A / B / C / D");
        }
        return g;
    }

    private String normalizeDisputeResolution(String resolution) {
        if (resolution == null) {
            throw new IllegalArgumentException("复核结论不能为空");
        }
        String r = resolution.trim().toUpperCase();
        if (!Set.of("UPHELD", "MODIFIED", "REVOKED", "MIXED").contains(r)) {
            throw new IllegalArgumentException("复核结论只能是 UPHELD / MODIFIED / REVOKED / MIXED");
        }
        return r;
    }

    private void validateDimensions(List<ObservationCreateRequest.ItemInput> items) {
        Set<String> dims = new HashSet<>();
        for (ObservationCreateRequest.ItemInput i : items) {
            if (!Set.of(DIM_CLASSROOM, DIM_CONTENT, DIM_INTERACTION, DIM_TIME).contains(i.getDimension())) {
                throw new IllegalArgumentException("未知观察维度：" + i.getDimension());
            }
            if (!dims.add(i.getDimension())) {
                throw new IllegalArgumentException("同一观察单的维度不能重复：" + dimensionLabel(i.getDimension()));
            }
        }
    }

    private void validateLevels(List<ObservationCreateRequest.ItemInput> items) {
        for (ObservationCreateRequest.ItemInput i : items) {
            if (!Set.of(LV_INTERNAL, LV_SENSITIVE, LV_IMPROVEMENT, LV_PUBLIC).contains(i.getFactLevel())) {
                throw new IllegalArgumentException("事实分级只能是 INTERNAL / SENSITIVE / IMPROVEMENT_RELATED / PUBLIC");
            }
            if (i.getSuggestionLevel() != null
                    && !Set.of(LV_INTERNAL, LV_SENSITIVE, LV_IMPROVEMENT, LV_PUBLIC).contains(i.getSuggestionLevel())) {
                throw new IllegalArgumentException("建议分级非法");
            }
        }
    }

    private String buildMasked(String text, String level, String sensitiveType) {
        if (LV_SENSITIVE.equals(level)) {
            return maskingService.mask(text, sensitiveType);
        }
        if (LV_INTERNAL.equals(level)) {
            return null;
        }
        // 即使是改进相关/公开层，也顺手去掉手机号等身份信息，避免误填泄露
        return maskingService.maskStudentIdentity(text);
    }

    private String sensitiveTypeOf(String level) {
        return LV_SENSITIVE.equals(level) ? SENS_STUDENT_ID : null;
    }

    private List<Long> parseItemIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
    }

    private Integer conclusionVersionNo(Long conclusionId) {
        return conclusionId == null ? null
                : conclusionRepository.findById(conclusionId).map(CycleConclusion::getVersionNo).orElse(null);
    }

    // ---------------- 权限 ----------------

    private String resolveViewLevel(TeachingObservation obs, AuthUser user) {
        if (user.hasRole(ROLE_SUPERVISOR) || user.hasRole(ROLE_COURSE_ADMIN) || user.hasRole(ROLE_ADMIN)) {
            return "FULL";
        }
        if (isBoundInstructor(obs, user)) {
            return "INSTRUCTOR";
        }
        return "PUBLIC";
    }

    private boolean isBoundInstructor(TeachingObservation obs, AuthUser user) {
        if (!user.hasRole(ROLE_INSTRUCTOR) || user.getUserId() == null) {
            return false;
        }
        return userRepository.findById(user.getUserId())
                .map(u -> obs.getInstructorId() != null && obs.getInstructorId().equals(u.getInstructorId()))
                .orElse(false);
    }

    private void requireInstructor(TeachingObservation obs, AuthUser user) {
        if (!isBoundInstructor(obs, user)) {
            throw new ForbiddenException("只有被观察讲师本人可以执行该操作");
        }
    }

    private void requireObserver(TeachingObservation obs, AuthUser user) {
        if (!user.hasRole(ROLE_SUPERVISOR) || !user.getUsername().equals(obs.getObserverUsername())) {
            throw new ForbiddenException("只有建立该观察的教学督导本人可以执行该操作");
        }
    }

    private void requireSupervisorOrInstructor(TeachingObservation obs, AuthUser user) {
        if (user.hasRole(ROLE_SUPERVISOR) || isBoundInstructor(obs, user)) {
            return;
        }
        throw new ForbiddenException("改进目标由督导与讲师共同维护，当前角色无权操作");
    }

    private void requireCourseAdmin(AuthUser user) {
        if (!user.hasRole(ROLE_COURSE_ADMIN)) {
            throw new ForbiddenException("只有课程负责人（课程管理员）可以执行该操作");
        }
    }

    // ---------------- 提醒 ----------------

    private boolean isSupervisorBroadcast(String type) {
        return Set.of(RMD_DISPUTE_REVIEW, RMD_INSTRUCTOR_REJECTED).contains(type);
    }

    private List<String> myOpenReminderTypes(Long obsId, AuthUser user) {
        return reminderRepository.findByObservationIdOrderByIdAsc(obsId).stream()
                .filter(r -> "OPEN".equals(r.getStatus()))
                .filter(r -> user.getUsername().equals(r.getAssigneeUsername())
                        || (user.hasRole(ROLE_SUPERVISOR) && isSupervisorBroadcast(r.getReminderType()))
                        || (user.hasRole(ROLE_COURSE_ADMIN)
                            && Set.of(RMD_OWNER_CLOSE, RMD_REOPEN_APPROVAL).contains(r.getReminderType())))
                .map(ObservationReminder::getReminderType)
                .distinct()
                .toList();
    }

    private void remind(TeachingObservation obs, String type, String dedupKey,
                        User assignee, String assigneeName, String message, LocalDateTime dueAt) {
        String assigneeUsername = assignee != null ? assignee.getUsername() : null;
        Optional<ObservationReminder> existing =
                reminderRepository.findByObservationIdAndReminderTypeAndDedupKey(obs.getId(), type, dedupKey);
        if (existing.isPresent()) {
            // 已存在同事实提醒（任何状态）：不重复产生
            return;
        }
        try {
            reminderRepository.saveAndFlush(ObservationReminder.builder()
                    .observationId(obs.getId())
                    .reminderType(type)
                    .dedupKey(dedupKey)
                    .assigneeUsername(assigneeUsername)
                    .assigneeName(assigneeName)
                    .message(message)
                    .dueAt(dueAt)
                    .status("OPEN")
                    .build());
        } catch (DataIntegrityViolationException dup) {
            // 并发双请求同时插入：唯一约束兜底，后到者静默放弃，提醒不重复
            log.debug("重复提醒被唯一约束拦截: obs={} type={} key={}", obs.getId(), type, dedupKey);
        }
    }

    private void resolveReminders(Long obsId, Set<String> types, String note) {
        for (ObservationReminder r : reminderRepository.findByObservationIdOrderByIdAsc(obsId)) {
            if ("OPEN".equals(r.getStatus()) && types.contains(r.getReminderType())) {
                r.setStatus("RESOLVED");
                r.setResolvedAt(LocalDateTime.now());
                r.setResolvedNote(note);
                reminderRepository.save(r);
            }
        }
    }

    private User instructorUser(Long instructorId) {
        return userRepository.findByInstructorId(instructorId).orElse(null);
    }

    private void remindInstructor(TeachingObservation obs, String type, String key, String msg, LocalDateTime due) {
        remind(obs, type, key, instructorUser(obs.getInstructorId()), obs.getInstructorName(), msg, due);
    }

    private void remindInstructorProgress(TeachingObservation obs, String msg) {
        remindInstructor(obs, RMD_GOAL_PROGRESS, "progress-" + obs.getImproveRound(), msg, null);
    }

    private void remindObserver(TeachingObservation obs, String type, String key, String msg, LocalDateTime due) {
        User observer = userRepository.findByUsername(obs.getObserverUsername()).orElse(null);
        remind(obs, type, key, observer, obs.getObserverName(), msg, due);
    }

    private void remindOwner(TeachingObservation obs, String type, String key, String msg) {
        User owner = obs.getOwnerUsername() != null
                ? userRepository.findByUsername(obs.getOwnerUsername()).orElse(null) : null;
        remind(obs, type, key, owner, obs.getOwnerName(), msg, null);
    }

    private void remindOwnerClose(TeachingObservation obs, String msg) {
        // 结案闸门触发每次都是新的“待结案轮次”：用结论版本数+延长次数构造单调去重键
        long conclusions = conclusionRepository.countByObservationId(obs.getId());
        String key = "close-c" + conclusions + "-e" + obs.getExtensionCount();
        remindOwner(obs, RMD_OWNER_CLOSE, key, msg);
    }

    private void remindSupervisors(TeachingObservation obs, String type, String key, String msg) {
        // 广播：assignee 留空，查询时对全体督导可见；去重仍靠 (obs,type,key)
        remind(obs, type, key, null, "全体教学督导", msg, null);
    }

    private void addEvent(Long obsId, String type, AuthUser user, String from, String to,
                          String refType, Long refId, String detail) {
        eventRepository.save(ObservationEvent.builder()
                .observationId(obsId)
                .eventType(type)
                .actorUsername(user != null ? user.getUsername() : "system")
                .actorName(user != null ? user.getRealName() : "系统")
                .fromStatus(from)
                .toStatus(to)
                .refType(refType)
                .refId(refId)
                .detail(detail)
                .build());
    }
}
