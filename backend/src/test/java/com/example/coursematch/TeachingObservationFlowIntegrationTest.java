package com.example.coursematch;

import com.example.coursematch.dto.request.TeachingObservationRequests.*;
import com.example.coursematch.dto.response.TeachingObservationResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.ForbiddenException;
import com.example.coursematch.service.TeachingObservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeachingObservationFlowIntegrationTest {

    @Autowired private TeachingObservationService service;
    @Autowired private TeachingObservationRepository observationRepository;
    @Autowired private ObservationItemRepository itemRepository;
    @Autowired private ObservationItemRevisionRepository revisionRepository;
    @Autowired private ObservationAddendumRepository addendumRepository;
    @Autowired private ObservationDisputeRepository disputeRepository;
    @Autowired private ObservationGoalRepository goalRepository;
    @Autowired private GoalEvidenceRepository evidenceRepository;
    @Autowired private ObservationCycleDecisionRepository decisionRepository;
    @Autowired private ObservationTimelineEventRepository eventRepository;
    @Autowired private ObservationReminderRepository reminderRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private InstructorRepository instructorRepository;

    private final AuthUser supervisor1 = AuthUser.builder().userId(101L).username("supervisor1")
            .realName("督导甲").roles(Set.of("SUPERVISOR")).build();
    private final AuthUser supervisor2 = AuthUser.builder().userId(102L).username("supervisor2")
            .realName("督导乙").roles(Set.of("SUPERVISOR")).build();
    private final AuthUser instructor = AuthUser.builder().userId(201L).username("instructor1")
            .realName("张明").instructorId(1001L).roles(Set.of("INSTRUCTOR")).build();
    private final AuthUser otherInstructor = AuthUser.builder().userId(202L).username("instructor2")
            .realName("李华").instructorId(1002L).roles(Set.of("INSTRUCTOR")).build();
    private final AuthUser owner = AuthUser.builder().userId(301L).username("course_owner")
            .realName("课程负责人").roles(Set.of("COURSE_OWNER")).build();
    private final AuthUser viewer = AuthUser.builder().userId(401L).username("viewer")
            .realName("查看者").roles(Set.of("VIEWER")).build();

    private Long courseId;
    private Long instructorId;

    @BeforeEach
    void setUp() {
        reminderRepository.deleteAll();
        eventRepository.deleteAll();
        decisionRepository.deleteAll();
        evidenceRepository.deleteAll();
        goalRepository.deleteAll();
        disputeRepository.deleteAll();
        addendumRepository.deleteAll();
        revisionRepository.deleteAll();
        itemRepository.deleteAll();
        observationRepository.deleteAll();
        courseRepository.deleteAll();
        instructorRepository.deleteAll();

        Course course = courseRepository.save(Course.builder()
                .name("带教测试课程").industry("IT").difficulty("高级").status("ACTIVE").build());
        Instructor teacher = instructorRepository.save(Instructor.builder()
                .name("张明").teachingYears(8).studentRating(4.6).status("ACTIVE")
                .available(true).maxCoursesPerMonth(4).currentMonthCourses(0).build());
        courseId = course.getId();
        instructorId = teacher.getId();
        instructor.setInstructorId(instructorId);
    }

    private CreateObservation createRequest(String sensitivity, String grade, String fact) {
        CreateObservation req = new CreateObservation();
        req.setCourseId(courseId);
        req.setInstructorId(instructorId);
        req.setSessionNo("2026-09-第1场");
        req.setOccurredAt(LocalDateTime.now().minusDays(1));
        req.setResponseDueAt(LocalDateTime.now().plusDays(3));
        req.setAttachmentSource("课堂录像#unit-1；听课表扫描件");
        req.setSummary("用于改进周期测试");
        req.setNeedsImprovement(true);
        ItemInput item = new ItemInput();
        item.setDimension("TIME_CONTROL");
        item.setSensitivityLevel(sensitivity);
        item.setGrade(grade);
        item.setFact(fact);
        item.setAdvice("建议使用分段计时并预留 5 分钟总结");
        req.setItems(List.of(item));
        return req;
    }

    @Test
    void gradedVisibilityMasksSensitiveFactsAndHidesDetailsFromViewer() {
        Long id = service.createObservation(createRequest("RESTRICTED", "NEEDS_IMPROVEMENT",
                "学员张伟投诉原文：课堂拖堂，电话13800138000"), supervisor1).id();

        TeachingObservationResponse instructorView = service.getObservation(id, instructor);
        assertEquals("INSTRUCTOR", instructorView.viewLevel());
        assertTrue(instructorView.items().get(0).masked());
        assertTrue(instructorView.items().get(0).fact().contains("已隐藏"));
        assertNull(instructorView.attachmentSource());

        TeachingObservationResponse publicView = service.getObservation(id, viewer);
        assertEquals("PUBLIC", publicView.viewLevel());
        assertTrue(publicView.items().isEmpty());
        assertNull(publicView.observerName());
        assertNull(publicView.summary());

        assertThrows(ForbiddenException.class, () -> service.getObservation(id, otherInstructor));
    }

    @Test
    void disputeMustBeReviewedByAnotherSupervisorAndKeepsOriginalTraceable() {
        Long id = service.createObservation(createRequest("INSTRUCTOR_RELATED", "NEEDS_IMPROVEMENT",
                "课后练习未讲解，13800138000"), supervisor1).id();
        Long itemId = itemRepository.findByObservationIdOrderByIdAsc(id).get(0).getId();

        DisputeRequest dispute = new DisputeRequest();
        dispute.setItemId(itemId);
        dispute.setReason("事实有误：课后练习已在最后十分钟讲解");
        service.raiseDispute(id, dispute, instructor);
        Long disputeId = disputeRepository.findByObservationIdOrderByRaisedAtDesc(id).get(0).getId();
        assertEquals("OBJECTION_PENDING_REVIEW", service.getObservation(id, supervisor1).status());

        assertThrows(ForbiddenException.class, () -> {
            DisputeReviewRequest review = reviewRequest(disputeId, "UPHELD", "原观察人不能复核");
            service.reviewDispute(id, review, supervisor1);
        });

        DisputeReviewRequest corrected = reviewRequest(disputeId, "CORRECTED", "录像显示练习确已讲解");
        corrected.setCorrectedFact("课后练习已讲解，但总结时间不足");
        corrected.setCorrectedGrade("QUALIFIED");
        corrected.setCorrectedAdvice("继续优化总结时间");
        TeachingObservationResponse after = service.reviewDispute(id, corrected, supervisor2);

        assertEquals("PENDING_ACK", after.status());
        assertEquals("QUALIFIED", after.items().get(0).grade());
        assertTrue(revisionRepository.findByObservationIdOrderByRevisionNoAscIdAsc(id).stream()
                .anyMatch(r -> "REVIEW_CORRECTED".equals(r.getChangeType())
                        && "NEEDS_IMPROVEMENT".equals(r.getGrade())));
        assertTrue(revisionRepository.findByObservationIdOrderByRevisionNoAscIdAsc(id).stream()
                .anyMatch(r -> "REVIEW_CORRECTED_CURRENT".equals(r.getChangeType())
                        && "QUALIFIED".equals(r.getGrade())));
    }

    @Test
    void fullImprovementEvidenceCloseAndReopenKeepsVersionChain() {
        Long id = service.createObservation(createRequest("INSTRUCTOR_RELATED", "NEEDS_IMPROVEMENT",
                "小组讨论超时，压缩了练习反馈"), supervisor1).id();
        service.acknowledge(id, new AcknowledgeRequest(), instructor);
        RespondRequest respond = new RespondRequest();
        respond.setResponseText("认可反馈，将按阶段改进");
        service.respond(id, respond, instructor);
        assertEquals("GOAL_SETTING", observationRepository.findById(id).orElseThrow().getStatus());

        GoalsRequest goals = new GoalsRequest();
        GoalInput goalInput = new GoalInput();
        goalInput.setStageNo(1);
        goalInput.setTitle("优化时间分配");
        goalInput.setMeasurementCriteria("连续一次课各环节偏差不超过5分钟，并提交课堂计时表");
        goalInput.setDueDate(LocalDate.now().plusDays(7));
        goalInput.setRequired(true);
        goals.setGoals(List.of(goalInput));
        service.saveGoals(id, goals, supervisor1);
        service.confirmGoals(id, null, instructor);
        TeachingObservationResponse activeGoals = service.confirmGoals(id, null, supervisor1);
        assertEquals("EVIDENCE_PENDING", activeGoals.status());

        Long goalId = goalRepository.findByObservationIdOrderByStageNoAscIdAsc(id).get(0).getId();
        EvidenceRequest evidence = new EvidenceRequest();
        evidence.setContent("已按四段式组织课堂，计时表显示偏差3分钟");
        evidence.setMaterialUri("evidence/timing-sheet.pdf");
        evidence.setClientToken("evidence-token-1");
        service.submitEvidence(goalId, evidence, instructor);
        assertEquals("FOLLOWUP_REVIEW", observationRepository.findById(id).orElseThrow().getStatus());

        Long evidenceId = evidenceRepository.findByGoalIdOrderBySubmittedAtDescIdDesc(goalId).get(0).getId();
        EvidenceReviewRequest review = new EvidenceReviewRequest();
        review.setEvidenceId(evidenceId);
        review.setDecision("APPROVE");
        review.setComment("计时表满足衡量标准");
        TeachingObservationResponse reviewed = service.reviewEvidence(id, review, supervisor1);
        assertEquals("AWAIT_COURSE_OWNER", reviewed.status());

        DecisionRequest staleClose = decisionRequest("close-token-1", "证据齐备");
        staleClose.setExpectedVersion(999L);
        assertThrows(DataIntegrityViolationException.class, () -> service.closeCycle(id, staleClose, owner));

        DecisionRequest close = decisionRequest("close-token-1", "计时表与复查意见均满足标准");
        close.setConclusion("改进目标达成，准予结案");
        TeachingObservationResponse closed = service.closeCycle(id, close, owner);
        assertEquals("CLOSED", closed.status());
        assertThrows(IllegalStateException.class,
                () -> service.closeCycle(id, decisionRequest("close-token-2", "重复点击"), owner));

        ReopenRequest reopen = new ReopenRequest();
        reopen.setReason("结案后发现计时表场次错误");
        reopen.setEvidenceError("证明材料关联到其他课程场次");
        reopen.setClientToken("reopen-token-1");
        service.requestReopen(id, reopen, instructor);

        ReopenDecisionRequest approve = new ReopenDecisionRequest();
        approve.setComment("证据错误属实，批准重开");
        approve.setClientToken("reopen-approve-token-1");
        service.approveReopen(id, approve, owner);
        assertEquals("REOPEN_REVIEW", observationRepository.findById(id).orElseThrow().getStatus());

        NewConclusionRequest newConclusion = new NewConclusionRequest();
        newConclusion.setConclusion("撤销原达标结论，重新安排一次课堂观察");
        newConclusion.setBasis("原计时表场次错误，不能作为有效证据");
        newConclusion.setReplacementNote("新结论替代 v1 结案结论，旧结论仅作历史追溯");
        newConclusion.setClientToken("new-conclusion-token-1");
        TeachingObservationResponse reopened = service.createReopenedConclusion(id, newConclusion, owner);

        assertEquals("CLOSED", reopened.status());
        List<ObservationCycleDecision> decisions =
                decisionRepository.findByObservationIdOrderByVersionNoAscIdAsc(id);
        assertEquals(3, decisions.size());
        ObservationCycleDecision oldClose = decisions.stream()
                .filter(d -> "CLOSE".equals(d.getDecisionType())).findFirst().orElseThrow();
        ObservationCycleDecision latest = decisions.stream()
                .filter(d -> "REOPENED_CONCLUSION".equals(d.getDecisionType())).findFirst().orElseThrow();
        assertEquals("SUPERSEDED", oldClose.getStatus());
        assertEquals("ACTIVE", latest.getStatus());
        assertEquals(oldClose.getId(), latest.getReplacesDecisionId());
        assertTrue(latest.getConclusion().contains("替代说明"));
    }

    @Test
    void addendumIsAppendOnlyAndCannotSilentlyReplaceOriginalItem() {
        Long id = service.createObservation(createRequest("PUBLIC", "QUALIFIED", "原始事实"), supervisor1).id();
        String originalFact = itemRepository.findByObservationIdOrderByIdAsc(id).get(0).getCurrentFact();

        AddendumInput addendum = new AddendumInput();
        addendum.setSensitivityLevel("INSTRUCTOR_RELATED");
        addendum.setContent("课后收到补充材料，仅作追加说明");
        service.addAddendum(id, addendum, supervisor2);

        assertEquals(1, addendumRepository.findByObservationIdOrderByCreatedAtAsc(id).size());
        assertEquals(originalFact, itemRepository.findByObservationIdOrderByIdAsc(id).get(0).getCurrentFact());
        assertTrue(eventRepository.findByObservationIdOrderByCreatedAtAscIdAsc(id).stream()
                .anyMatch(e -> "ADDENDUM_APPENDED".equals(e.getEventType())));
    }

    private DisputeReviewRequest reviewRequest(Long disputeId, String decision, String comment) {
        DisputeReviewRequest req = new DisputeReviewRequest();
        req.setDisputeId(disputeId);
        req.setDecision(decision);
        req.setReviewComment(comment);
        return req;
    }

    private DecisionRequest decisionRequest(String token, String basis) {
        DecisionRequest req = new DecisionRequest();
        req.setClientToken(token);
        req.setBasis(basis);
        return req;
    }
}
