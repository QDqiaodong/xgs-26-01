package com.example.coursematch.service;

import com.example.coursematch.dto.request.TeachingObservationRequests.*;
import com.example.coursematch.dto.response.ObservationSummaryResponse;
import com.example.coursematch.dto.response.TeachingObservationResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.CurrentUserProvider;
import com.example.coursematch.security.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TeachingObservationService {

    private static final String SUPERVISOR = "SUPERVISOR";
    private static final String COURSE_OWNER = "COURSE_OWNER";
    private static final String INSTRUCTOR = "INSTRUCTOR";
    private static final String VIEWER = "VIEWER";
    private static final String ADMIN = "ADMIN";

    private static final Set<String> DIMENSIONS = Set.of("ORGANIZATION", "ACCURACY", "INTERACTION", "TIME_CONTROL", "OTHER");
    private static final Set<String> GRADES = Set.of("EXCELLENT", "QUALIFIED", "NEEDS_IMPROVEMENT", "SERIOUS_ISSUE");
    private static final Set<String> SENSITIVITIES = Set.of("PUBLIC", "INSTRUCTOR_RELATED", "RESTRICTED");

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(?:\\d{17}[0-9Xx]|\\d{15})(?!\\d)");
    private static final Pattern STUDENT_NAME = Pattern.compile("学员[一-龥]{2,4}");

    private final TeachingObservationRepository observationRepository;
    private final ObservationItemRepository itemRepository;
    private final ObservationItemRevisionRepository revisionRepository;
    private final ObservationAddendumRepository addendumRepository;
    private final ObservationDisputeRepository disputeRepository;
    private final ObservationGoalRepository goalRepository;
    private final GoalEvidenceRepository evidenceRepository;
    private final ObservationCycleDecisionRepository decisionRepository;
    private final ObservationTimelineEventRepository eventRepository;
    private final ObservationReminderRepository reminderRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;

    // ============================ 观察创建与补充（原文不可变） ============================

    @Transactional
    public TeachingObservationResponse createObservation(CreateObservation req, AuthUser user) {
        requireRole(user, SUPERVISOR);
        Course course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + req.getCourseId()));
        Instructor instructor = instructorRepository.findById(req.getInstructorId())
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + req.getInstructorId()));
        validateOccurredAt(req.getOccurredAt());
        if (req.getItems().stream().anyMatch(i -> !DIMENSIONS.contains(i.getDimension())
                || !GRADES.contains(i.getGrade()) || !SENSITIVITIES.contains(i.getSensitivityLevel()))) {
            throw new IllegalArgumentException("观察维度、等级或敏感级别不合法");
        }

        TeachingObservation obs = observationRepository.save(TeachingObservation.builder()
                .courseId(course.getId())
                .assignmentId(req.getAssignmentId())
                .courseName(course.getName())
                .sessionNo(req.getSessionNo())
                .instructorId(instructor.getId())
                .instructorName(instructor.getName())
                .observerUserId(user.getUserId())
                .observerUsername(user.getUsername())
                .observerName(user.getRealName())
                .occurredAt(req.getOccurredAt())
                .attachmentSource(req.getAttachmentSource())
                .summary(req.getSummary())
                .needsImprovement(Boolean.TRUE.equals(req.getNeedsImprovement()))
                .responseDueAt(req.getResponseDueAt())
                .status("PENDING_ACK")
                .build());

        for (ItemInput input : req.getItems()) {
            ObservationItem item = itemRepository.save(ObservationItem.builder()
                    .observationId(obs.getId())
                    .dimension(input.getDimension())
                    .sensitivityLevel(input.getSensitivityLevel())
                    .currentFact(input.getFact())
                    .grade(input.getGrade())
                    .currentAdvice(input.getAdvice())
                    .effectiveStatus("ACTIVE")
                    .revisionNo(1)
                    .build());
            ObservationItemRevision rev = saveRevision(item, "ORIGINAL", user.getRealName(), "初始观察记录", null);
            item.setLatestRevisionId(rev.getId());
            itemRepository.save(item);
        }

        addEvent(obs.getId(), "OBSERVATION_CREATED", "OBSERVATION", obs.getId(),
                "按课程场次建立带教观察，等待讲师确认与回应", user);
        createReminder(obs, "ACK_DUE", "INSTRUCTOR", obs.getInstructorId(),
                "请确认带教观察", "讲师需在截止时间前确认已阅并回应；如有事实错误可先提出异议。",
                "ACK:" + obs.getId(), obs.getResponseDueAt());
        return toResponse(obs, user);
    }

    @Transactional
    public TeachingObservationResponse addAddendum(Long id, AddendumInput req, AuthUser user) {
        requireRole(user, SUPERVISOR);
        TeachingObservation obs = lock(id);
        if (!SENSITIVITIES.contains(req.getSensitivityLevel())) {
            throw new IllegalArgumentException("敏感级别不合法");
        }
        ObservationAddendum addendum = addendumRepository.save(ObservationAddendum.builder()
                .observationId(id)
                .sensitivityLevel(req.getSensitivityLevel())
                .content(req.getContent())
                .authorUserId(user.getUserId())
                .authorUsername(user.getUsername())
                .authorName(user.getRealName())
                .build());
        addEvent(id, "ADDENDUM_APPENDED", "ADDENDUM", addendum.getId(),
                "事后补充已追加，原始观察项未被修改", user);
        return getObservation(id, user);
    }

    // ============================ 讲师确认、回应、拒绝、异议 ============================

    @Transactional
    public TeachingObservationResponse acknowledge(Long id, AcknowledgeRequest req, AuthUser user) {
        requireInstructor(user);
        TeachingObservation obs = lockOwned(id, user);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"PENDING_ACK".equals(obs.getStatus()) || obs.getAcknowledgedAt() != null) {
            throw new IllegalStateException("当前状态不能确认，或观察已确认/存在异议");
        }
        obs.setAcknowledgedAt(LocalDateTime.now());
        resolveReminders(id, Set.of("ACK_DUE"));
        addEvent(id, "INSTRUCTOR_ACKNOWLEDGED", "OBSERVATION", id, "讲师确认已阅，等待回应", user);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse respond(Long id, RespondRequest req, AuthUser user) {
        requireInstructor(user);
        TeachingObservation obs = lockOwned(id, user);
        ensureExpected(obs, req.getExpectedVersion());
        if (!Set.of("PENDING_ACK", "INSTRUCTOR_OVERDUE").contains(obs.getStatus())) {
            throw new IllegalStateException("当前状态不能提交回应：" + statusText(obs.getStatus()));
        }
        if (obs.getAcknowledgedAt() == null) {
            obs.setAcknowledgedAt(LocalDateTime.now());
        }
        obs.setResponseText(req.getResponseText());
        obs.setRespondedAt(LocalDateTime.now());
        obs.setStatus(Boolean.TRUE.equals(obs.getNeedsImprovement()) ? "GOAL_SETTING" : "AWAIT_COURSE_OWNER");
        resolveReminders(id, Set.of("ACK_DUE", "RESPONSE_OVERDUE"));
        if (Boolean.TRUE.equals(obs.getNeedsImprovement())) {
            addEvent(id, "INSTRUCTOR_RESPONDED", "OBSERVATION", id, "讲师已回应，督导与讲师共同制定分阶段目标", user);
        } else {
            addEvent(id, "INSTRUCTOR_RESPONDED", "OBSERVATION", id, "讲师已回应，无需改进目标，等待课程负责人结案确认", user);
            createReminder(obs, "CLOSE_REQUIRED", "COURSE_OWNER", null,
                    "观察周期待结案", "该观察无需改进，课程负责人可核对后结案。", "NO_GOAL_CLOSE:" + id, null);
        }
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse refuse(Long id, RefuseRequest req, AuthUser user) {
        requireInstructor(user);
        TeachingObservation obs = lockOwned(id, user);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"PENDING_ACK".equals(obs.getStatus()) || obs.getAcknowledgedAt() != null) {
            throw new IllegalStateException("仅确认前可以拒绝确认");
        }
        obs.setStatus("INSTRUCTOR_REFUSED");
        obs.setRefusedAt(LocalDateTime.now());
        obs.setRefuseReason(req.getReason());
        resolveReminders(id, Set.of("ACK_DUE"));
        addEvent(id, "INSTRUCTOR_REFUSED", "OBSERVATION", id, "讲师拒绝确认：" + req.getReason(), user);
        createReminder(obs, "INSTRUCTOR_REFUSED", "COURSE_OWNER", null,
                "讲师拒绝确认观察", "需课程负责人跟进拒绝原因并决定后续处理。", "REFUSED:" + id, null);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse raiseDispute(Long id, DisputeRequest req, AuthUser user) {
        requireInstructor(user);
        TeachingObservation obs = lockOwned(id, user);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"PENDING_ACK".equals(obs.getStatus()) || obs.getAcknowledgedAt() != null) {
            throw new IllegalStateException("讲师只能在确认前对未处理观察提出异议");
        }
        if (disputeRepository.existsByObservationIdAndStatus(id, "OPEN")) {
            throw new IllegalStateException("已有待复核异议，请勿重复提交");
        }
        if (req.getItemId() != null) {
            ObservationItem item = itemRepository.findById(req.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("观察项不存在: " + req.getItemId()));
            if (!item.getObservationId().equals(id)) {
                throw new IllegalArgumentException("异议观察项不属于该观察");
            }
        }
        ObservationDispute dispute = disputeRepository.save(ObservationDispute.builder()
                .observationId(id)
                .itemId(req.getItemId())
                .reason(req.getReason())
                .raisedBy(user.getUsername())
                .raisedByName(user.getRealName())
                .status("OPEN")
                .build());
        obs.setStatus("OBJECTION_PENDING_REVIEW");
        resolveReminders(id, Set.of("ACK_DUE"));
        addEvent(id, "DISPUTE_RAISED", "DISPUTE", dispute.getId(),
                "讲师在确认前提出事实异议，等待另一名督导复核", user);
        createReminder(obs, "DISPUTE_REVIEW", "OTHER_SUPERVISOR", null,
                "观察异议待复核", "须由非原观察人的教学督导复核，原观察人不能自行关闭异议。",
                "DISPUTE:" + dispute.getId(), null);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse reviewDispute(Long id, DisputeReviewRequest req, AuthUser user) {
        requireRole(user, SUPERVISOR);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        ObservationDispute dispute = disputeRepository.findById(req.getDisputeId())
                .orElseThrow(() -> new EntityNotFoundException("异议不存在: " + req.getDisputeId()));
        if (!dispute.getObservationId().equals(id) || !"OPEN".equals(dispute.getStatus())) {
            throw new IllegalStateException("异议不存在或已处理，重复提交不会再次改变状态");
        }
        if (Objects.equals(obs.getObserverUserId(), user.getUserId())) {
            throw new ForbiddenException("原观察人不能复核或关闭针对自己观察的异议");
        }

        ObservationItem item = null;
        if (dispute.getItemId() != null) {
            item = itemRepository.findById(dispute.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("观察项不存在: " + dispute.getItemId()));
        }
        String decision = req.getDecision();
        switch (decision) {
            case "UPHELD" -> {
                if (item != null) {
                    item.setRevisionNo(item.getRevisionNo() + 1);
                    ObservationItemRevision upheld = saveCurrentItemRevision(item, "REVIEW_UPHELD",
                            user.getRealName(), req.getReviewComment(), dispute.getId());
                    item.setLatestRevisionId(upheld.getId());
                    itemRepository.save(item);
                }
                dispute.setStatus("UPHELD");
            }
            case "CORRECTED" -> {
                if (item == null) throw new IllegalArgumentException("针对整份观察的异议不支持直接修正具体项");
                if (isBlank(req.getCorrectedFact()) || isBlank(req.getCorrectedGrade())) {
                    throw new IllegalArgumentException("修正观察项必须提供修正后的事实与等级");
                }
                if (!GRADES.contains(req.getCorrectedGrade())) {
                    throw new IllegalArgumentException("修正等级不合法");
                }
                String newSensitivity = isBlank(req.getCorrectedSensitivityLevel())
                        ? item.getSensitivityLevel() : req.getCorrectedSensitivityLevel();
                if (!SENSITIVITIES.contains(newSensitivity)) {
                    throw new IllegalArgumentException("修正敏感级别不合法");
                }
                ObservationItemRevision old = saveHistoricalItemRevision(item, item.getRevisionNo(),
                        "REVIEW_CORRECTED", user.getRealName(), req.getReviewComment(), dispute.getId());
                item.setRevisionNo(item.getRevisionNo() + 1);
                item.setCurrentFact(req.getCorrectedFact());
                item.setGrade(req.getCorrectedGrade());
                item.setCurrentAdvice(req.getCorrectedAdvice());
                item.setSensitivityLevel(newSensitivity);
                item.setEffectiveStatus("ACTIVE");
                ObservationItemRevision current = saveCurrentItemRevision(item, "REVIEW_CORRECTED_CURRENT", user.getRealName(),
                        req.getReviewComment(), dispute.getId());
                item.setLatestRevisionId(current.getId());
                itemRepository.save(item);
                dispute.setStatus("CORRECTED");
            }
            case "REVOKED" -> {
                if (item == null) throw new IllegalArgumentException("针对整份观察的异议不支持直接撤销具体项");
                ObservationItemRevision old = saveHistoricalItemRevision(item, item.getRevisionNo(),
                        "REVIEW_REVOKED", user.getRealName(), req.getReviewComment(), dispute.getId());
                item.setRevisionNo(item.getRevisionNo() + 1);
                item.setEffectiveStatus("REVOKED");
                ObservationItemRevision revokeCurrent = saveCurrentItemRevision(item, "REVIEW_REVOKED_CURRENT",
                        user.getRealName(), req.getReviewComment(), dispute.getId());
                item.setLatestRevisionId(revokeCurrent.getId());
                itemRepository.save(item);
                dispute.setStatus("REVOKED");
            }
            default -> throw new IllegalArgumentException("复核决定必须为 UPHELD/CORRECTED/REVOKED");
        }
        dispute.setReviewerUserId(user.getUserId());
        dispute.setReviewerUsername(user.getUsername());
        dispute.setReviewerName(user.getRealName());
        dispute.setReviewComment(req.getReviewComment());
        dispute.setReviewedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        obs.setNeedsImprovement(hasActiveImprovementItem(id));
        obs.setStatus("PENDING_ACK");
        resolveReminders(id, Set.of("DISPUTE_REVIEW"));
        addEvent(id, "DISPUTE_REVIEWED", "DISPUTE", dispute.getId(),
                "另一名督导完成异议复核：" + disputeStatusText(dispute.getStatus())
                        + "。原文字、原等级与处理意见已保留版本", user);
        createReminder(obs, "ACK_DUE", "INSTRUCTOR", obs.getInstructorId(),
                "异议已复核，请继续确认观察", "请查看当前有效观察项后确认或回应。",
                "ACK_AFTER_DISPUTE:" + dispute.getId(), obs.getResponseDueAt());
        return getObservation(id, user);
    }

    // ============================ 分阶段目标与证据回报 ============================

    @Transactional
    public TeachingObservationResponse saveGoals(Long id, GoalsRequest req, AuthUser user) {
        boolean supervisor = user.hasRole(SUPERVISOR);
        boolean instructor = user.hasRole(INSTRUCTOR);
        if (!supervisor && !instructor) {
            throw new ForbiddenException("仅督导或讲师可共同形成改进目标");
        }
        TeachingObservation obs = lock(id);
        if (instructor && !supervisor && !Objects.equals(obs.getInstructorId(), user.getInstructorId())) {
            throw new ForbiddenException("只能参与本人观察的改进目标");
        }
        ensureExpected(obs, req.getExpectedVersion());
        if (!"GOAL_SETTING".equals(obs.getStatus())) {
            throw new IllegalStateException("仅目标制定阶段可以保存阶段目标，当前状态：" + statusText(obs.getStatus()));
        }
        if (!hasActiveImprovementItem(id)) {
            throw new IllegalStateException("当前没有待改进的有效观察项；被撤销或修正后不再需要目标，不能创建改进目标");
        }
        List<ObservationGoal> existing = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(id);
        Set<Integer> existingStages = new HashSet<>();
        for (ObservationGoal old : existing) {
            if (!"DRAFT".equals(old.getStatus()) && !"PROPOSED".equals(old.getStatus())) {
                existingStages.add(old.getStageNo());
            }
        }
        goalRepository.deleteAll(existing.stream()
                .filter(g -> "DRAFT".equals(g.getStatus()) || "PROPOSED".equals(g.getStatus())).toList());
        goalRepository.flush();

        Set<Integer> stages = new HashSet<>();
        boolean activate = Boolean.TRUE.equals(req.getGoals().stream().findFirst().map(GoalInput::getActivate).orElse(false));
        boolean bothRoles = supervisor && instructor;
        for (GoalInput input : req.getGoals()) {
            if (!stages.add(input.getStageNo())) {
                throw new IllegalArgumentException("阶段序号不能重复：" + input.getStageNo());
            }
            if (existingStages.contains(input.getStageNo())) {
                throw new IllegalArgumentException("阶段 " + input.getStageNo() + " 已存在，延长周期只能追加新阶段，不能覆盖旧目标");
            }
            if (input.getDueDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("目标截止日期不能早于今天");
            }
            LocalDateTime now = LocalDateTime.now();
            ObservationGoal.ObservationGoalBuilder builder = ObservationGoal.builder()
                    .observationId(id)
                    .stageNo(input.getStageNo())
                    .title(input.getTitle())
                    .measurementCriteria(input.getMeasurementCriteria())
                    .dueDate(input.getDueDate())
                    .required(input.getRequired() == null || input.getRequired())
                    .createdBy(user.getUsername())
                    .createdByName(user.getRealName());
            if (activate && bothRoles) {
                builder.status("ACTIVE").activatedAt(now);
                if (supervisor) {
                    builder.supervisorConfirmedBy(user.getUsername()).supervisorConfirmedAt(now);
                }
                if (instructor) {
                    builder.instructorConfirmedBy(user.getUsername()).instructorConfirmedAt(now);
                }
            } else {
                builder.status("PROPOSED");
            }
            goalRepository.save(builder.build());
        }
        if (activate) {
            if (supervisor && instructor) {
                obs.setStatus("EVIDENCE_PENDING");
                addEvent(id, "GOALS_ACTIVATED", "GOAL", null,
                        "督导与讲师共同确认 " + req.getGoals().size() + " 个分阶段目标", user);
            } else if (supervisor) {
                createReminder(obs, "GOAL_AGREEMENT", "INSTRUCTOR", obs.getInstructorId(),
                        "阶段目标待讲师确认", "请核对分阶段目标、衡量标准、截止时间和证明材料要求。",
                        "GOAL_AGREE:" + req.getGoals().stream().map(GoalInput::getStageNo).findFirst().orElse(1), null);
                addEvent(id, "GOALS_PROPOSED_BY_SUPERVISOR", "GOAL", null, "督导已拟定阶段目标，等待讲师确认", user);
            } else {
                addEvent(id, "GOALS_PROPOSED_BY_INSTRUCTOR", "GOAL", null, "讲师已确认拟定目标，等待督导确认", user);
            }
        } else {
            addEvent(id, "GOALS_DRAFTED", "GOAL", null, "阶段目标已暂存，尚需双方确认", user);
        }
        resolveReminders(id, Set.of("GOAL_SETTING_DUE"));
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse confirmGoals(Long id, Long expectedVersion, AuthUser user) {
        boolean supervisor = user.hasRole(SUPERVISOR);
        boolean instructor = user.hasRole(INSTRUCTOR);
        if (!supervisor && !instructor) {
            throw new ForbiddenException("仅督导或讲师可确认改进目标");
        }
        TeachingObservation obs = lock(id);
        ensureExpected(obs, expectedVersion);
        if (instructor && !supervisor && !Objects.equals(obs.getInstructorId(), user.getInstructorId())) {
            throw new ForbiddenException("只能确认本人观察的改进目标");
        }
        if (!"GOAL_SETTING".equals(obs.getStatus())) {
            throw new IllegalStateException("当前状态不能确认目标");
        }
        List<ObservationGoal> goals = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(id);
        if (goals.isEmpty()) {
            throw new IllegalStateException("尚未形成任何阶段目标");
        }
        LocalDateTime now = LocalDateTime.now();
        for (ObservationGoal goal : goals) {
            if (!Set.of("PROPOSED", "ACTIVE").contains(goal.getStatus())) {
                throw new IllegalStateException("目标 " + goal.getStageNo() + " 已进入证据阶段，不能再确认");
            }
            if (supervisor) {
                goal.setSupervisorConfirmedBy(user.getUsername());
                goal.setSupervisorConfirmedAt(now);
            }
            if (instructor) {
                goal.setInstructorConfirmedBy(user.getUsername());
                goal.setInstructorConfirmedAt(now);
            }
        }
        boolean bothConfirmed = goals.stream().allMatch(g ->
                g.getSupervisorConfirmedAt() != null && g.getInstructorConfirmedAt() != null);
        if (bothConfirmed) {
            goals.forEach(g -> {
                g.setStatus("ACTIVE");
                g.setActivatedAt(now);
                goalRepository.save(g);
            });
            obs.setStatus("EVIDENCE_PENDING");
            resolveReminders(id, Set.of("GOAL_AGREEMENT"));
            addEvent(id, "GOALS_ACTIVATED", "GOAL", null, "督导与讲师共同确认全部阶段目标，开始证据回报", user);
        } else {
            goals.forEach(g -> {
                g.setStatus("PROPOSED");
                goalRepository.save(g);
            });
            if (supervisor) {
                createReminder(obs, "GOAL_AGREEMENT", "INSTRUCTOR", obs.getInstructorId(),
                        "阶段目标待讲师确认", "请核对目标、衡量标准、截止时间和证明材料。",
                        "GOAL_AGREE:" + goals.get(0).getStageNo(), null);
            }
            addEvent(id, "GOALS_PARTIALLY_CONFIRMED", "GOAL", null, "一方已确认阶段目标，等待另一方确认", user);
        }
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse submitEvidence(Long goalId, EvidenceRequest req, AuthUser user) {
        requireRole(user, INSTRUCTOR);
        ObservationGoal goal = lockGoal(goalId);
        TeachingObservation obs = lock(goal.getObservationId());
        if (!Objects.equals(obs.getInstructorId(), user.getInstructorId())) {
            throw new ForbiddenException("只能回报本人目标进展");
        }
        ensureExpected(obs, req.getExpectedVersion());
        if (!Set.of("EVIDENCE_PENDING", "GOAL_OVERDUE").contains(obs.getStatus())
                || Set.of("APPROVED", "SUBMITTED", "SUPERSEDED").contains(goal.getStatus())) {
            throw new IllegalStateException("当前目标/周期状态不能提交证明，或已等待督导复查");
        }
        Optional<GoalEvidence> duplicate = evidenceRepository.findByGoalIdAndClientToken(goalId, req.getClientToken());
        if (duplicate.isPresent()) {
            throw new DataIntegrityViolationException("证明材料已提交，请勿重复提交");
        }

        GoalEvidence evidence = evidenceRepository.save(GoalEvidence.builder()
                .goalId(goalId)
                .observationId(obs.getId())
                .clientToken(req.getClientToken())
                .content(req.getContent())
                .materialUri(req.getMaterialUri())
                .submittedBy(user.getUsername())
                .submittedByName(user.getRealName())
                .status("SUBMITTED")
                .build());
        goal.setStatus("SUBMITTED");
        goalRepository.save(goal);
        obs.setStatus("FOLLOWUP_REVIEW");
        if (obs.getFollowupDueAt() == null || obs.getFollowupDueAt().isBefore(LocalDateTime.now())) {
            obs.setFollowupDueAt(LocalDateTime.now().plusDays(7));
        }
        obs.setLastReviewedAt(null);
        resolveReminders(obs.getId(), Set.of("GOAL_OVERDUE", "EVIDENCE_SUPPLEMENT"));
        addEvent(obs.getId(), "EVIDENCE_SUBMITTED", "EVIDENCE", evidence.getId(),
                "讲师回报阶段进展与证明材料，等待督导复查", user);
        createReminder(obs, "EVIDENCE_REVIEW", "OBSERVER", obs.getObserverUserId(),
                "证明材料待复查", "请核对讲师提交的证明是否满足衡量标准。",
                "EVIDENCE:" + evidence.getId(), obs.getFollowupDueAt());
        return toResponse(obs, user);
    }

    @Transactional
    public TeachingObservationResponse reviewEvidence(Long id, EvidenceReviewRequest req, AuthUser user) {
        requireRole(user, SUPERVISOR);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        GoalEvidence evidence = evidenceRepository.findById(req.getEvidenceId())
                .orElseThrow(() -> new EntityNotFoundException("证明不存在: " + req.getEvidenceId()));
        if (!evidence.getObservationId().equals(id) || !"SUBMITTED".equals(evidence.getStatus())) {
            throw new IllegalStateException("证明不存在或已复查，重复点击不会产生第二条复查意见");
        }
        ObservationGoal goal = goalRepository.findById(evidence.getGoalId())
                .orElseThrow(() -> new EntityNotFoundException("目标不存在"));

        switch (req.getDecision()) {
            case "SUPPLEMENT" -> {
                evidence.setStatus("SUPPLEMENT_REQUESTED");
                goal.setStatus("REWORKED");
                if (req.getNewDueDate() != null) goal.setDueDate(req.getNewDueDate());
                obs.setStatus("EVIDENCE_PENDING");
                createReminder(obs, "EVIDENCE_SUPPLEMENT", "INSTRUCTOR", obs.getInstructorId(),
                        "证明材料需补充", req.getComment(), "SUPPLEMENT:" + evidence.getId(), null);
            }
            case "REJECT" -> {
                evidence.setStatus("REJECTED");
                goal.setStatus("REWORKED");
                obs.setStatus("EVIDENCE_PENDING");
                createReminder(obs, "EVIDENCE_SUPPLEMENT", "INSTRUCTOR", obs.getInstructorId(),
                        "证明不充分已退回", req.getComment(), "REJECT_EVIDENCE:" + evidence.getId(), null);
            }
            case "APPROVE" -> {
                evidence.setStatus("APPROVED");
                goal.setStatus("APPROVED");
                goal.setCompletedAt(LocalDateTime.now());
                goalRepository.save(goal);
                boolean pendingRequired = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(id).stream()
                        .filter(g -> Boolean.TRUE.equals(g.getRequired()))
                        .anyMatch(g -> !"APPROVED".equals(g.getStatus()));
                obs.setStatus(pendingRequired ? "EVIDENCE_PENDING" : "AWAIT_COURSE_OWNER");
                if (!pendingRequired) {
                    createReminder(obs, "CLOSE_REQUIRED", "COURSE_OWNER", null,
                            "必需目标证据均有效，待负责人结案",
                            "课程负责人核对有效证据后可决定结案、延长或升级。", "READY_CLOSE:" + id, null);
                }
            }
            default -> throw new IllegalArgumentException("处理决定必须为 SUPPLEMENT/REJECT/APPROVE");
        }
        evidence.setReviewComment(req.getComment());
        evidence.setReviewedBy(user.getUsername());
        evidence.setReviewedByName(user.getRealName());
        evidence.setReviewedAt(LocalDateTime.now());
        evidenceRepository.save(evidence);
        goalRepository.save(goal);
        obs.setLastReviewedAt(LocalDateTime.now());
        resolveReminders(id, Set.of("EVIDENCE_REVIEW", "SUPERVISOR_REVIEW_OVERDUE"));
        addEvent(id, "EVIDENCE_REVIEWED", "EVIDENCE", evidence.getId(),
                "督导复查证明：" + evidence.getStatus(), user);
        return getObservation(id, user);
    }

    // ============================ 负责人结案/延长/升级/重开 ============================

    @Transactional
    public TeachingObservationResponse closeCycle(Long id, DecisionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"AWAIT_COURSE_OWNER".equals(obs.getStatus())) {
            throw new IllegalStateException("仅待负责人决策状态可以结案，重复点击不会重复结案");
        }
        boolean hasGoals = !goalRepository.findByObservationIdOrderByStageNoAscIdAsc(id).isEmpty();
        long pendingRequired = goalRepository.countByObservationIdAndRequiredTrueAndStatusNot(id, "APPROVED");
        if (hasGoals && pendingRequired > 0) {
            throw new IllegalStateException("仍有必需目标缺少有效证据，不能结案");
        }
        String conclusion = isBlank(req.getConclusion())
                ? (hasGoals ? "各必需阶段目标均已有有效证明，周期达到结案条件。" : "本次观察无需改进，周期结案。")
                : req.getConclusion();
        ObservationCycleDecision decision = saveDecision(obs, "CLOSE", conclusion, req.getBasis(),
                req.getClientToken(), user);
        obs.setStatus("CLOSED");
        obs.setClosedAt(LocalDateTime.now());
        obs.setCurrentConclusionId(decision.getId());
        resolveAllReminders(id);
        addEvent(id, "CYCLE_CLOSED", "DECISION", decision.getId(), "课程负责人在证据齐备后结案", user);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse extendCycle(Long id, DecisionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!Set.of("AWAIT_COURSE_OWNER", "EVIDENCE_PENDING", "GOAL_OVERDUE", "FOLLOWUP_REVIEW",
                "SUPERVISOR_REVIEW_OVERDUE", "ESCALATION_REQUESTED").contains(obs.getStatus())) {
            throw new IllegalStateException("当前状态不能延长周期");
        }
        ObservationCycleDecision decision = saveDecision(obs, "EXTEND",
                isBlank(req.getConclusion()) ? "基于持续风险延长改进周期。" : req.getConclusion(),
                req.getBasis(), req.getClientToken(), user);
        obs.setStatus("GOAL_SETTING");
        obs.setFollowupDueAt(null);
        resolveReminders(id, Set.of("CLOSE_REQUIRED", "ESCALATION_REQUESTED", "EVIDENCE_REVIEW",
                "SUPERVISOR_REVIEW_OVERDUE", "GOAL_OVERDUE"));
        addEvent(id, "CYCLE_EXTENDED", "DECISION", decision.getId(),
                "课程负责人依据持续风险延长周期，需追加阶段目标", user);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse requestEscalation(Long id, DecisionRequest req, AuthUser user) {
        requireRole(user, SUPERVISOR);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (Set.of("CLOSED", "INTENSIVE_COACHING", "REOPEN_PENDING", "REOPEN_REVIEW").contains(obs.getStatus())) {
            throw new IllegalStateException("当前状态不能申请升级");
        }
        if ("ESCALATION_REQUESTED".equals(obs.getStatus())) {
            throw new IllegalStateException("升级申请已提交，等待课程负责人处理");
        }
        obs.setStatus("ESCALATION_REQUESTED");
        addEvent(id, "ESCALATION_REQUESTED", "OBSERVATION", id,
                "督导基于风险申请升级为强化辅导：" + req.getBasis(), user);
        createReminder(obs, "ESCALATION_REQUESTED", "COURSE_OWNER", null,
                "强化辅导升级待处理", req.getBasis(), "ESCALATION_REQ:" + id, null);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse escalateCycle(Long id, DecisionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!Set.of("AWAIT_COURSE_OWNER", "EVIDENCE_PENDING", "GOAL_OVERDUE", "FOLLOWUP_REVIEW",
                "SUPERVISOR_REVIEW_OVERDUE", "ESCALATION_REQUESTED").contains(obs.getStatus())) {
            throw new IllegalStateException("当前状态不能升级为强化辅导");
        }
        ObservationCycleDecision decision = saveDecision(obs, "ESCALATE",
                isBlank(req.getConclusion()) ? "升级为强化辅导。" : req.getConclusion(),
                req.getBasis(), req.getClientToken(), user);
        obs.setStatus("INTENSIVE_COACHING");
        obs.setCurrentConclusionId(decision.getId());
        resolveAllReminders(id);
        addEvent(id, "CYCLE_ESCALATED", "DECISION", decision.getId(),
                "课程负责人依据写明的风险依据升级为强化辅导", user);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse requestReopen(Long id, ReopenRequest req, AuthUser user) {
        if (!user.hasAnyRole(SUPERVISOR, INSTRUCTOR, COURSE_OWNER)) {
            throw new ForbiddenException("当前角色无权申请重开");
        }
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (user.hasRole(INSTRUCTOR) && !user.hasRole(SUPERVISOR)
                && !Objects.equals(obs.getInstructorId(), user.getInstructorId())) {
            throw new ForbiddenException("只能申请重开本人的观察周期");
        }
        if (!"CLOSED".equals(obs.getStatus())) {
            throw new IllegalStateException("仅已结案周期可申请重开");
        }
        obs.setStatus("REOPEN_PENDING");
        addEvent(id, "REOPEN_REQUESTED", "OBSERVATION", id,
                "结案后发现证据错误，申请重开。错误：" + req.getEvidenceError(), user);
        createReminder(obs, "REOPEN_APPROVAL", "COURSE_OWNER", null,
                "结案重开申请待批准", req.getReason() + "；证据错误：" + req.getEvidenceError(),
                "REOPEN_REQ:" + req.getClientToken(), null);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse approveReopen(Long id, ReopenDecisionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"REOPEN_PENDING".equals(obs.getStatus())) {
            throw new IllegalStateException("当前没有待批准的重开申请");
        }
        obs.setStatus("REOPEN_REVIEW");
        obs.setClosedAt(null);
        obs.setFollowupDueAt(LocalDateTime.now().plusDays(7));
        resolveReminders(id, Set.of("REOPEN_APPROVAL"));
        ObservationCycleDecision approval = saveDecision(obs, "REOPEN_APPROVAL",
                "批准重开并恢复待复查", req.getComment(),
                req.getClientToken() == null ? UUID.randomUUID().toString() : req.getClientToken(), user);
        addEvent(id, "REOPEN_APPROVED", "DECISION", approval.getId(),
                "课程负责人批准重开，原结论保留为历史版本，恢复到待复查", user);
        createReminder(obs, "REOPEN_REVIEW", "OBSERVER", obs.getObserverUserId(),
                "重开周期待复查", "请复核证据错误并形成新结论；旧结论不得覆盖。",
                "REOPEN_REVIEW:" + id, obs.getFollowupDueAt());
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse declineReopen(Long id, ReopenDecisionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"REOPEN_PENDING".equals(obs.getStatus())) {
            throw new IllegalStateException("当前没有待批准的重开申请");
        }
        obs.setStatus("CLOSED");
        obs.setClosedAt(LocalDateTime.now());
        resolveReminders(id, Set.of("REOPEN_APPROVAL"));
        addEvent(id, "REOPEN_DECLINED", "OBSERVATION", id, "课程负责人未批准重开：" + req.getComment(), user);
        return getObservation(id, user);
    }

    @Transactional
    public TeachingObservationResponse createReopenedConclusion(Long id, NewConclusionRequest req, AuthUser user) {
        requireRole(user, COURSE_OWNER);
        TeachingObservation obs = lock(id);
        ensureExpected(obs, req.getExpectedVersion());
        if (!"REOPEN_REVIEW".equals(obs.getStatus())) {
            throw new IllegalStateException("仅重开待复查状态可以登记新结论");
        }
        Long oldDecisionId = null;
        if (obs.getCurrentConclusionId() != null) {
            ObservationCycleDecision old = decisionRepository.findById(obs.getCurrentConclusionId()).orElse(null);
            if (old != null && "ACTIVE".equals(old.getStatus())) {
                old.setStatus("SUPERSEDED");
                decisionRepository.save(old);
                oldDecisionId = old.getId();
            }
        }
        String conclusion = req.getConclusion() + "（替代说明：" + req.getReplacementNote() + "）";
        ObservationCycleDecision decision = saveDecision(obs, "REOPENED_CONCLUSION", conclusion,
                req.getBasis(), req.getClientToken(), user, oldDecisionId);
        obs.setStatus("CLOSED");
        obs.setClosedAt(LocalDateTime.now());
        obs.setCurrentConclusionId(decision.getId());
        resolveAllReminders(id);
        addEvent(id, "REOPENED_CONCLUSION_CREATED", "DECISION", decision.getId(),
                "重开后形成新结论，旧结论作为历史版本保留，未被覆盖", user);
        return getObservation(id, user);
    }

    // ============================ 查询（服务端分级可见） ============================

    @Transactional(readOnly = true)
    public List<ObservationSummaryResponse> list(AuthUser user, Long courseId, Long instructorId, String status) {
        List<TeachingObservation> list = courseId != null
                ? observationRepository.findByCourseIdOrderByOccurredAtDesc(courseId)
                : instructorId != null
                ? observationRepository.findByInstructorIdOrderByOccurredAtDesc(instructorId)
                : observationRepository.findAllByOrderByOccurredAtDesc();
        List<ObservationSummaryResponse> result = new ArrayList<>();
        for (TeachingObservation obs : list) {
            if (status != null && !status.equals(obs.getStatus())) continue;
            if (user.hasRole(INSTRUCTOR) && !user.hasAnyRole(SUPERVISOR, COURSE_OWNER, ADMIN)
                    && !Objects.equals(obs.getInstructorId(), user.getInstructorId())) continue;
            if (instructorId != null && !Objects.equals(obs.getInstructorId(), instructorId)) continue;
            List<ObservationReminder> reminders = reminderRepository.findByObservationIdOrderByCreatedAtDesc(obs.getId());
            result.add(toSummary(obs, viewLevel(user, obs), reminders));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public TeachingObservationResponse getObservation(Long id, AuthUser user) {
        TeachingObservation obs = observationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("观察不存在: " + id));
        return toResponse(obs, user);
    }

    @Transactional(readOnly = true)
    public List<TeachingObservationResponse.ReminderDto> myPendingReminders(AuthUser user) {
        CurrentUserProvider.require();
        return reminderRepository.findByStatusOrderByCreatedAtDesc("PENDING").stream()
                .filter(r -> reminderVisible(r, user))
                .map(this::toReminderDto)
                .toList();
    }

    // ============================ 逾期扫描：显式状态，不合并为“处理中” ============================

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    @Transactional
    public void scanOverdue() {
        LocalDateTime now = LocalDateTime.now();
        for (TeachingObservation obs : observationRepository.findByStatusAndResponseDueAtBefore("PENDING_ACK", now)) {
            if (obs.getResponseDueAt() != null && obs.getAcknowledgedAt() == null) {
                obs.setStatus("INSTRUCTOR_OVERDUE");
                observationRepository.save(obs);
                createReminder(obs, "RESPONSE_OVERDUE", "INSTRUCTOR", obs.getInstructorId(),
                        "讲师逾期未回应", "回应期限已过，需尽快说明；课程负责人将看到该待处理状态。",
                        "OVERDUE_RESPONSE:" + obs.getId(), obs.getResponseDueAt());
                createReminder(obs, "INSTRUCTOR_OVERDUE", "COURSE_OWNER", null,
                        "讲师逾期未回应", "观察回应已逾期，需要跟进。", "OWNER_OVERDUE_RESPONSE:" + obs.getId(),
                        obs.getResponseDueAt());
                addEvent(obs.getId(), "RESPONSE_OVERDUE", "OBSERVATION", obs.getId(),
                        "超过回应期限，进入讲师逾期未回应状态", AuthUser.builder().username("system").realName("系统").build());
            }
        }
        for (ObservationGoal goal : goalRepository.findAll()) {
            TeachingObservation obs = observationRepository.findById(goal.getObservationId()).orElse(null);
            if (obs != null && "EVIDENCE_PENDING".equals(obs.getStatus())
                    && Set.of("ACTIVE", "REWORKED").contains(goal.getStatus())
                    && goal.getDueDate() != null && goal.getDueDate().isBefore(LocalDate.now())) {
                obs.setStatus("GOAL_OVERDUE");
                observationRepository.save(obs);
                createReminder(obs, "GOAL_OVERDUE", "INSTRUCTOR", obs.getInstructorId(),
                        "阶段目标逾期", "目标「" + goal.getTitle() + "」已超过证明截止日期。",
                        "OVERDUE_GOAL:" + goal.getId(), goal.getDueDate().atStartOfDay());
            }
        }
        for (TeachingObservation obs : observationRepository.findByStatusAndFollowupDueAtBefore("FOLLOWUP_REVIEW", now)) {
            obs.setStatus("SUPERVISOR_REVIEW_OVERDUE");
            observationRepository.save(obs);
            createReminder(obs, "SUPERVISOR_REVIEW_OVERDUE", "OBSERVER", obs.getObserverUserId(),
                    "督导长期未复查", "讲师证明已提交但超过复查期限，需要督导尽快处理。",
                    "OVERDUE_REVIEW:" + obs.getId(), obs.getFollowupDueAt());
            addEvent(obs.getId(), "SUPERVISOR_REVIEW_OVERDUE", "OBSERVATION", obs.getId(),
                    "督导超过复查期限，进入督导长期未复查状态",
                    AuthUser.builder().username("system").realName("系统").build());
        }
    }

    // ============================ 私有辅助 ============================

    private TeachingObservation lock(Long id) {
        return observationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("观察不存在: " + id));
    }

    private ObservationGoal lockGoal(Long id) {
        return goalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("目标不存在: " + id));
    }

    private TeachingObservation lockOwned(Long id, AuthUser user) {
        TeachingObservation obs = lock(id);
        if (!Objects.equals(obs.getInstructorId(), user.getInstructorId())) {
            throw new ForbiddenException("只能处理本人的带教观察");
        }
        return obs;
    }

    private void requireRole(AuthUser user, String role) {
        if (!user.hasRole(role)) {
            throw new ForbiddenException("当前角色无权执行该操作");
        }
    }

    private void requireInstructor(AuthUser user) {
        requireRole(user, INSTRUCTOR);
        if (user.getInstructorId() == null) {
            throw new ForbiddenException("当前讲师账号未关联讲师档案");
        }
    }

    private boolean isInstructorOnly(AuthUser user) {
        return user.hasRole(INSTRUCTOR) && !user.hasAnyRole(SUPERVISOR, COURSE_OWNER, ADMIN);
    }

    private void ensureExpected(TeachingObservation obs, Long expected) {
        if (expected != null && !Objects.equals(obs.getVersionLock(), expected)) {
            throw new DataIntegrityViolationException("页面数据已过期，请刷新后再操作；旧内容未覆盖较新结果");
        }
    }

    private void validateOccurredAt(LocalDateTime occurredAt) {
        if (occurredAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("授课发生时间不能晚于当前时间");
        }
    }

    private boolean hasActiveImprovementItem(Long observationId) {
        return itemRepository.findByObservationIdOrderByIdAsc(observationId).stream()
                .anyMatch(i -> "ACTIVE".equals(i.getEffectiveStatus())
                        && Set.of("NEEDS_IMPROVEMENT", "SERIOUS_ISSUE").contains(i.getGrade()));
    }

    private ObservationItemRevision saveRevision(ObservationItem item, String type, String changedBy,
                                                 String reason, Long disputeId) {
        int versionRevisionNo = item.getRevisionNo();
        return revisionRepository.save(ObservationItemRevision.builder()
                .itemId(item.getId())
                .observationId(item.getObservationId())
                .revisionNo(versionRevisionNo)
                .dimension(item.getDimension())
                .sensitivityLevel(item.getSensitivityLevel())
                .factText(item.getCurrentFact())
                .grade(item.getGrade())
                .adviceText(item.getCurrentAdvice())
                .changeType(type)
                .changedBy(changedBy)
                .changeReason(reason)
                .disputeId(disputeId)
                .build());
    }

    private ObservationItemRevision saveHistoricalItemRevision(ObservationItem item, Integer revisionNo, String type,
                                                               String changedBy, String reason, Long disputeId) {
        return revisionRepository.save(ObservationItemRevision.builder()
                .itemId(item.getId())
                .observationId(item.getObservationId())
                .revisionNo(revisionNo)
                .dimension(item.getDimension())
                .sensitivityLevel(item.getSensitivityLevel())
                .factText(item.getCurrentFact())
                .grade(item.getGrade())
                .adviceText(item.getCurrentAdvice())
                .changeType(type)
                .changedBy(changedBy)
                .changeReason(reason)
                .disputeId(disputeId)
                .build());
    }

    private ObservationItemRevision saveCurrentItemRevision(ObservationItem item, String type, String changedBy,
                                                            String reason, Long disputeId) {
        return revisionRepository.save(ObservationItemRevision.builder()
                .itemId(item.getId())
                .observationId(item.getObservationId())
                .revisionNo(item.getRevisionNo())
                .dimension(item.getDimension())
                .sensitivityLevel(item.getSensitivityLevel())
                .factText(item.getCurrentFact())
                .grade(item.getGrade())
                .adviceText(item.getCurrentAdvice())
                .changeType(type)
                .changedBy(changedBy)
                .changeReason(reason)
                .disputeId(disputeId)
                .build());
    }

    private ObservationCycleDecision saveDecision(TeachingObservation obs, String type, String conclusion,
                                                  String basis, String clientToken, AuthUser user) {
        return saveDecision(obs, type, conclusion, basis, clientToken, user, null);
    }

    private ObservationCycleDecision saveDecision(TeachingObservation obs, String type, String conclusion,
                                                  String basis, String clientToken, AuthUser user,
                                                  Long replacesDecisionId) {
        if (!isBlank(clientToken)) {
            Optional<ObservationCycleDecision> duplicate =
                    decisionRepository.findByObservationIdAndClientToken(obs.getId(), clientToken);
            if (duplicate.isPresent()) {
                throw new DataIntegrityViolationException("该决策请求已处理，请勿重复提交");
            }
        }
        if (Set.of("CLOSE", "ESCALATE", "REOPENED_CONCLUSION").contains(type)) {
            decisionRepository.findByObservationIdOrderByVersionNoAscIdAsc(obs.getId()).stream()
                    .filter(d -> "ACTIVE".equals(d.getStatus()) && !Objects.equals(d.getId(), replacesDecisionId))
                    .forEach(d -> {
                        d.setStatus("SUPERSEDED");
                        decisionRepository.save(d);
                    });
        }
        int versionNo = (int) decisionRepository.countByObservationId(obs.getId()) + 1;
        return decisionRepository.save(ObservationCycleDecision.builder()
                .observationId(obs.getId())
                .versionNo(versionNo)
                .decisionType(type)
                .conclusion(conclusion)
                .basis(basis)
                .decidedBy(user.getUsername())
                .decidedByName(user.getRealName())
                .replacesDecisionId(replacesDecisionId)
                .status("ACTIVE")
                .clientToken(clientToken)
                .build());
    }

    private void addEvent(Long observationId, String type, String sourceType, Long sourceId,
                          String detail, AuthUser user) {
        String eventKey = type + ":" + sourceType + ":" + (sourceId == null ? UUID.randomUUID() : sourceId);
        if (eventRepository.existsByObservationIdAndEventKey(observationId, eventKey)) {
            return;
        }
        eventRepository.save(ObservationTimelineEvent.builder()
                .observationId(observationId)
                .eventType(type)
                .eventKey(eventKey)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .detail(detail)
                .actorUsername(user == null ? null : user.getUsername())
                .actorName(user == null ? null : user.getRealName())
                .build());
    }

    private void createReminder(TeachingObservation obs, String type, String role, Long assigneeId,
                                String title, String message, String dedupeKey, LocalDateTime dueAt) {
        Optional<ObservationReminder> existing =
                reminderRepository.findByObservationIdAndReminderTypeAndDedupeKey(obs.getId(), type, dedupeKey);
        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            return;
        }
        reminderRepository.save(ObservationReminder.builder()
                .observationId(obs.getId())
                .reminderType(type)
                .assigneeRole(role)
                .assigneeUserId(assigneeId)
                .title(title)
                .message(message)
                .dedupeKey(dedupeKey)
                .dueAt(dueAt)
                .status("PENDING")
                .build());
    }

    private void resolveReminders(Long observationId, Set<String> types) {
        reminderRepository.findByObservationIdOrderByCreatedAtDesc(observationId).stream()
                .filter(r -> types.contains(r.getReminderType()) && "PENDING".equals(r.getStatus()))
                .forEach(r -> {
                    r.setStatus("HANDLED");
                    r.setHandledAt(LocalDateTime.now());
                    reminderRepository.save(r);
                });
    }

    private void resolveAllReminders(Long observationId) {
        resolveReminders(observationId, reminderRepository.findByObservationIdOrderByCreatedAtDesc(observationId)
                .stream().map(ObservationReminder::getReminderType).collect(java.util.stream.Collectors.toSet()));
    }

    private String viewLevel(AuthUser user, TeachingObservation obs) {
        if (user.hasAnyRole(SUPERVISOR, COURSE_OWNER, ADMIN)) return "FULL";
        if (user.hasRole(INSTRUCTOR) && Objects.equals(obs.getInstructorId(), user.getInstructorId())) return "INSTRUCTOR";
        if (user.hasRole(INSTRUCTOR)) throw new ForbiddenException("不能查看其他讲师的观察周期");
        return "PUBLIC";
    }

    private boolean reminderVisible(ObservationReminder r, AuthUser user) {
        if (user.hasAnyRole(SUPERVISOR, COURSE_OWNER, ADMIN)) return true;
        if (user.hasRole(INSTRUCTOR)) {
            TeachingObservation obs = observationRepository.findById(r.getObservationId()).orElse(null);
            return obs != null && Objects.equals(obs.getInstructorId(), user.getInstructorId())
                    && "INSTRUCTOR".equals(r.getAssigneeRole());
        }
        return false;
    }

    private ObservationSummaryResponse toSummary(TeachingObservation obs, String level,
                                                 List<ObservationReminder> reminders) {
        String conclusion = currentConclusion(obs);
        long pending = reminders.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        return new ObservationSummaryResponse(
                obs.getId(), obs.getCourseId(), obs.getCourseName(), obs.getSessionNo(),
                obs.getInstructorId(), obs.getInstructorName(),
                "FULL".equals(level) ? obs.getObserverName() : null,
                obs.getOccurredAt(), obs.getStatus(), statusText(obs.getStatus()),
                Boolean.TRUE.equals(obs.getNeedsImprovement()),
                "PUBLIC".equals(level) ? conclusion : (conclusion != null ? conclusion : null),
                pending, obs.getVersionLock(), level);
    }

    private TeachingObservationResponse toResponse(TeachingObservation obs, AuthUser user) {
        String level = viewLevel(user, obs);
        List<ObservationItem> items = itemRepository.findByObservationIdOrderByIdAsc(obs.getId());
        List<ObservationItemRevision> revisions =
                revisionRepository.findByObservationIdOrderByRevisionNoAscIdAsc(obs.getId());
        List<ObservationAddendum> addenda = addendumRepository.findByObservationIdOrderByCreatedAtAsc(obs.getId());
        List<ObservationDispute> disputes = disputeRepository.findByObservationIdOrderByRaisedAtDesc(obs.getId());
        List<ObservationGoal> goals = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(obs.getId());
        List<GoalEvidence> evidenceAll = evidenceRepository.findByObservationIdOrderBySubmittedAtDescIdDesc(obs.getId());
        List<ObservationCycleDecision> decisions =
                decisionRepository.findByObservationIdOrderByVersionNoAscIdAsc(obs.getId());
        List<ObservationTimelineEvent> events =
                eventRepository.findByObservationIdOrderByCreatedAtAscIdAsc(obs.getId());
        List<ObservationReminder> reminders = reminderRepository.findByObservationIdOrderByCreatedAtDesc(obs.getId());

        List<TeachingObservationResponse.ItemDto> itemDtos = "PUBLIC".equals(level) ? List.of()
                : items.stream().map(i -> toItemDto(i, level)).toList();
        List<TeachingObservationResponse.RevisionDto> revisionDtos = "PUBLIC".equals(level) ? List.of()
                : revisions.stream().map(r -> toRevisionDto(r, level)).toList();
        List<TeachingObservationResponse.AddendumDto> addendumDtos = "PUBLIC".equals(level) ? List.of()
                : addenda.stream().map(a -> toAddendumDto(a, level)).toList();
        List<TeachingObservationResponse.DisputeDto> disputeDtos = "PUBLIC".equals(level) ? List.of()
                : disputes.stream().map(this::toDisputeDto).toList();
        List<TeachingObservationResponse.GoalDto> goalDtos = "PUBLIC".equals(level) ? List.of()
                : goals.stream().map(g -> toGoalDto(g, evidenceAll, level)).toList();
        List<TeachingObservationResponse.DecisionDto> decisionDtos = decisions.stream()
                .filter(d -> "PUBLIC".equals(level)
                        ? Set.of("CLOSE", "ESCALATE", "REOPENED_CONCLUSION").contains(d.getDecisionType())
                        : true)
                .map(this::toDecisionDto).toList();
        List<TeachingObservationResponse.EventDto> eventDtos = events.stream()
                .filter(e -> !"PUBLIC".equals(level) || Set.of("CYCLE_CLOSED", "CYCLE_ESCALATED",
                        "REOPENED_CONCLUSION_CREATED", "CYCLE_EXTENDED").contains(e.getEventType()))
                .map(e -> new TeachingObservationResponse.EventDto(e.getId(), e.getEventType(), e.getSourceType(),
                        e.getSourceId(), visibleText(e.getDetail(), level), e.getActorName(), e.getCreatedAt()))
                .toList();
        List<TeachingObservationResponse.ReminderDto> reminderDtos = reminders.stream()
                .filter(r -> !"PUBLIC".equals(level))
                .filter(r -> !"INSTRUCTOR".equals(level) || "INSTRUCTOR".equals(r.getAssigneeRole()))
                .map(this::toReminderDto).toList();

        return new TeachingObservationResponse(
                obs.getId(), obs.getCourseId(), obs.getAssignmentId(), obs.getCourseName(), obs.getSessionNo(),
                obs.getInstructorId(), obs.getInstructorName(),
                "FULL".equals(level) ? obs.getObserverName() : null,
                "FULL".equals(level) ? obs.getObserverUsername() : null,
                obs.getOccurredAt(), "FULL".equals(level) ? obs.getAttachmentSource() : null,
                obs.getStatus(), statusText(obs.getStatus()), Boolean.TRUE.equals(obs.getNeedsImprovement()),
                "FULL".equals(level) ? obs.getSummary() : null, currentConclusion(obs),
                obs.getCurrentConclusionId(), obs.getVersionLock(), level,
                obs.getResponseDueAt(), obs.getFollowupDueAt(), obs.getAcknowledgedAt(),
                "PUBLIC".equals(level) ? null : obs.getResponseText(), obs.getRespondedAt(), obs.getClosedAt(),
                itemDtos, revisionDtos, addendumDtos, disputeDtos, goalDtos, decisionDtos, eventDtos, reminderDtos);
    }

    private TeachingObservationResponse.ItemDto toItemDto(ObservationItem i, String level) {
        if ("FULL".equals(level)) {
            return new TeachingObservationResponse.ItemDto(i.getId(), i.getDimension(), i.getSensitivityLevel(),
                    i.getCurrentFact(), i.getGrade(), i.getCurrentAdvice(), i.getEffectiveStatus(),
                    i.getRevisionNo(), false);
        }
        if ("RESTRICTED".equals(i.getSensitivityLevel())) {
            boolean adviceVisible = Set.of("NEEDS_IMPROVEMENT", "SERIOUS_ISSUE").contains(i.getGrade());
            return new TeachingObservationResponse.ItemDto(i.getId(), i.getDimension(), i.getSensitivityLevel(),
                    "【已隐藏：涉及学员身份、投诉原文或内部判断】", i.getGrade(),
                    adviceVisible ? maskText(i.getCurrentAdvice()) : null, i.getEffectiveStatus(),
                    i.getRevisionNo(), true);
        }
        return new TeachingObservationResponse.ItemDto(i.getId(), i.getDimension(), i.getSensitivityLevel(),
                maskText(i.getCurrentFact()), i.getGrade(), maskText(i.getCurrentAdvice()),
                i.getEffectiveStatus(), i.getRevisionNo(), true);
    }

    private TeachingObservationResponse.RevisionDto toRevisionDto(ObservationItemRevision r, String level) {
        if ("FULL".equals(level)) {
            return new TeachingObservationResponse.RevisionDto(r.getId(), r.getItemId(), r.getRevisionNo(),
                    r.getDimension(), r.getSensitivityLevel(), r.getFactText(), r.getGrade(), r.getAdviceText(),
                    r.getChangeType(), r.getChangedBy(), r.getChangeReason(), r.getDisputeId(), r.getCreatedAt(), false);
        }
        if ("RESTRICTED".equals(r.getSensitivityLevel())) {
            return new TeachingObservationResponse.RevisionDto(r.getId(), r.getItemId(), r.getRevisionNo(),
                    r.getDimension(), r.getSensitivityLevel(), "【历史受限内容已隐藏】", r.getGrade(), null,
                    r.getChangeType(), r.getChangedBy(), null, r.getDisputeId(), r.getCreatedAt(), true);
        }
        return new TeachingObservationResponse.RevisionDto(r.getId(), r.getItemId(), r.getRevisionNo(),
                r.getDimension(), r.getSensitivityLevel(), maskText(r.getFactText()), r.getGrade(),
                maskText(r.getAdviceText()), r.getChangeType(), r.getChangedBy(), maskText(r.getChangeReason()),
                r.getDisputeId(), r.getCreatedAt(), true);
    }

    private TeachingObservationResponse.AddendumDto toAddendumDto(ObservationAddendum a, String level) {
        if ("FULL".equals(level)) {
            return new TeachingObservationResponse.AddendumDto(a.getId(), a.getSensitivityLevel(), a.getContent(),
                    a.getAuthorName(), a.getCreatedAt(), false);
        }
        if ("RESTRICTED".equals(a.getSensitivityLevel())) {
            return new TeachingObservationResponse.AddendumDto(a.getId(), a.getSensitivityLevel(),
                    "【受限补充说明已隐藏】", a.getAuthorName(), a.getCreatedAt(), true);
        }
        return new TeachingObservationResponse.AddendumDto(a.getId(), a.getSensitivityLevel(),
                maskText(a.getContent()), a.getAuthorName(), a.getCreatedAt(), true);
    }

    private TeachingObservationResponse.DisputeDto toDisputeDto(ObservationDispute d) {
        return new TeachingObservationResponse.DisputeDto(d.getId(), d.getItemId(), d.getReason(),
                d.getRaisedByName(), d.getRaisedAt(), d.getStatus(), d.getReviewerName(),
                d.getReviewComment(), d.getReviewedAt());
    }

    private TeachingObservationResponse.GoalDto toGoalDto(ObservationGoal g, List<GoalEvidence> all, String level) {
        List<TeachingObservationResponse.EvidenceDto> evidence = all.stream()
                .filter(e -> e.getGoalId().equals(g.getId()))
                .map(e -> new TeachingObservationResponse.EvidenceDto(e.getId(),
                        visibleText(e.getContent(), level), "FULL".equals(level) ? e.getMaterialUri() : maskUri(e.getMaterialUri()),
                        e.getSubmittedByName(), e.getSubmittedAt(), e.getStatus(),
                        visibleText(e.getReviewComment(), level), e.getReviewedByName(), e.getReviewedAt()))
                .toList();
        return new TeachingObservationResponse.GoalDto(g.getId(), g.getStageNo(), g.getTitle(),
                g.getMeasurementCriteria(), g.getDueDate(), g.getRequired(), g.getStatus(), g.getActivatedAt(),
                g.getCompletedAt(), evidence);
    }

    private TeachingObservationResponse.DecisionDto toDecisionDto(ObservationCycleDecision d) {
        return new TeachingObservationResponse.DecisionDto(d.getId(), d.getVersionNo(), d.getDecisionType(),
                d.getConclusion(), d.getBasis(), d.getDecidedByName(), d.getReplacesDecisionId(),
                d.getStatus(), d.getCreatedAt());
    }

    private TeachingObservationResponse.ReminderDto toReminderDto(ObservationReminder r) {
        return new TeachingObservationResponse.ReminderDto(r.getId(), r.getReminderType(), r.getAssigneeRole(),
                r.getTitle(), r.getMessage(), r.getStatus(), r.getDueAt(), r.getCreatedAt());
    }

    private String currentConclusion(TeachingObservation obs) {
        if (Set.of("REOPEN_PENDING", "REOPEN_REVIEW").contains(obs.getStatus())) {
            return null;
        }
        if (obs.getCurrentConclusionId() == null) return null;
        return decisionRepository.findById(obs.getCurrentConclusionId())
                .map(ObservationCycleDecision::getConclusion).orElse(null);
    }

    private String visibleText(String text, String level) {
        if (text == null || "FULL".equals(level)) return text;
        return maskText(text);
    }

    private String maskUri(String uri) {
        return uri == null ? null : "【附件链接仅督导/负责人可见】";
    }

    private String maskText(String text) {
        if (text == null) return null;
        String result = EMAIL.matcher(text).replaceAll("***@***");
        result = MOBILE.matcher(result).replaceAll("***********");
        result = ID_CARD.matcher(result).replaceAll("******************");
        result = STUDENT_NAME.matcher(result).replaceAll("学员**");
        return result;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String statusText(String s) {
        return switch (s) {
            case "PENDING_ACK" -> "待讲师确认回应";
            case "OBJECTION_PENDING_REVIEW" -> "异议待另一督导复核";
            case "GOAL_SETTING" -> "待共同制定阶段目标";
            case "EVIDENCE_PENDING" -> "待讲师回报证据";
            case "FOLLOWUP_REVIEW" -> "待督导复查证明";
            case "AWAIT_COURSE_OWNER" -> "待课程负责人决策";
            case "CLOSED" -> "已结案";
            case "REOPEN_PENDING" -> "重开申请待批准";
            case "REOPEN_REVIEW" -> "重开待复查";
            case "INTENSIVE_COACHING" -> "强化辅导中";
            case "INSTRUCTOR_REFUSED" -> "讲师拒绝确认";
            case "INSTRUCTOR_OVERDUE" -> "讲师逾期未回应";
            case "SUPERVISOR_REVIEW_OVERDUE" -> "督导长期未复查";
            case "GOAL_OVERDUE" -> "阶段目标逾期";
            case "ESCALATION_REQUESTED" -> "升级申请待负责人处理";
            default -> s;
        };
    }

    private String disputeStatusText(String s) {
        return switch (s) {
            case "UPHELD" -> "维持原观察项";
            case "CORRECTED" -> "修正观察项";
            case "REVOKED" -> "撤销观察项";
            default -> s;
        };
    }
}
