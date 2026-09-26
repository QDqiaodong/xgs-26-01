package com.example.coursematch;

import com.example.coursematch.dto.request.PlanCreateFromRecordRequest;
import com.example.coursematch.dto.response.PlanAssignmentResultResponse;
import com.example.coursematch.dto.response.PlanResponse;
import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.service.MatchPlanService;
import com.example.coursematch.service.PlanAssignmentService;
import com.example.coursematch.service.PlanConflictException;
import com.example.coursematch.service.PlanInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 推演方案“评审发布”完整链路集成测试（H2 + MySQL 兼容模式）。
 */
@SpringBootTest
class MatchPlanFlowIntegrationTest {

    @Autowired private MatchPlanService planService;
    @Autowired private PlanAssignmentService planAssignmentService;
    @Autowired private PlanInvalidationService invalidationService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private WeightConfigRepository weightConfigRepository;
    @Autowired private MatchRecordRepository matchRecordRepository;
    @Autowired private MatchRecordDetailRepository detailRepository;
    @Autowired private MatchPlanRepository planRepository;
    @Autowired private MatchPlanCandidateRepository candidateRepository;
    @Autowired private PlanReviewRepository reviewRepository;
    @Autowired private PlanEventRepository eventRepository;
    @Autowired private PublishedPlanSlotRepository slotRepository;
    @Autowired private CourseInstructorAssignmentRepository assignmentRepository;
    @Autowired private TransactionTemplate tx;

    private Long courseId;
    private Long weightId;
    private Long topInstructorId;
    private Long secondInstructorId;
    private Long recordId;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        eventRepository.deleteAll();
        reviewRepository.deleteAll();
        candidateRepository.deleteAll();
        planRepository.deleteAll();
        slotRepository.deleteAll();
        detailRepository.deleteAll();
        matchRecordRepository.deleteAll();
        assignmentRepository.deleteAll();
        instructorRepository.deleteAll();
        courseRepository.deleteAll();
        weightConfigRepository.deleteAll();

        Course course = courseRepository.save(Course.builder()
                .name("测试课程").industry("IT").difficulty("高级")
                .requiredQualifications("Java认证").styleRequirements("实战")
                .status("ACTIVE").build());
        courseId = course.getId();

        WeightConfig weight = weightConfigRepository.save(WeightConfig.builder()
                .name("测试权重").teachingYearsWeight(0.2).qualificationWeight(0.25)
                .ratingWeight(0.2).expertiseWeight(0.2).styleWeight(0.15)
                .gapThreshold(50.0).active(true).build());
        weightId = weight.getId();

        Instructor top = instructorRepository.save(Instructor.builder()
                .name("张三").teachingYears(10).studentRating(4.8)
                .certificates("Java认证").expertiseAreas("IT").teachingStyle("实战")
                .status("ACTIVE").available(true).maxCoursesPerMonth(4).currentMonthCourses(0)
                .build());
        Instructor second = instructorRepository.save(Instructor.builder()
                .name("李四").teachingYears(5).studentRating(4.2)
                .certificates("其他").expertiseAreas("IT").teachingStyle("理论")
                .status("ACTIVE").available(true).maxCoursesPerMonth(4).currentMonthCourses(0)
                .build());
        topInstructorId = top.getId();
        secondInstructorId = second.getId();

        MatchRecord record = matchRecordRepository.save(MatchRecord.builder()
                .courseId(courseId).courseName(course.getName()).courseIndustry(course.getIndustry())
                .weightConfigId(weightId).teachingYearsWeight(0.2).qualificationWeight(0.25)
                .ratingWeight(0.2).expertiseWeight(0.2).styleWeight(0.15)
                .hasGap(false).bestScore(90.0).matchCount(2).build());
        recordId = record.getId();

        saveDetail(recordId, top.getId(), top.getName(), 90.0, 1);
        saveDetail(recordId, second.getId(), second.getName(), 70.0, 2);
    }

    private void saveDetail(Long rid, Long iid, String name, double total, int rank) {
        detailRepository.save(MatchRecordDetail.builder()
                .recordId(rid).instructorId(iid).instructorName(name)
                .totalScore(total).teachingYearsScore(80.0).qualificationScore(90.0)
                .ratingScore(85.0).expertiseScore(88.0).styleScore(82.0).rank(rank)
                .build());
    }

    private Long createApprovedPlan() {
        PlanCreateFromRecordRequest req = new PlanCreateFromRecordRequest();
        req.setMatchRecordId(recordId);
        PlanResponse draft = planService.createDraft(req, "course_admin", "课程管理员");
        assertEquals("DRAFT", draft.getStatus());
        assertEquals(2, draft.getCandidateCount());

        Long id = draft.getId();
        planService.submit(id, "course_admin", "课程管理员");
        planService.reviewByInstructorAdmin(id, "APPROVED", "资质与可授课状态无误",
                "instructor_admin", "讲师管理员");
        planService.reviewByCourseAdmin(id, "APPROVED", "课程要求与权重无误",
                "course_admin", "课程管理员");
        return id;
    }

    // ---------------- 快照固化 ----------------

    @Test
    void draftFreezesSnapshotAndScores() {
        PlanCreateFromRecordRequest req = new PlanCreateFromRecordRequest();
        req.setMatchRecordId(recordId);
        PlanResponse draft = planService.createDraft(req, "course_admin", "课程管理员");

        // 讲师资料随后变化，方案快照评分/资料不变
        tx.executeWithoutResult(s -> {
            Instructor top = instructorRepository.findById(topInstructorId).orElseThrow();
            top.setAvailable(false);
            top.setStudentRating(2.0);
            instructorRepository.save(top);
        });

        PlanResponse reloaded = planService.getPlan(draft.getId());
        var topCandidate = reloaded.getCandidates().stream()
                .filter(c -> c.getRankNo() == 1).findFirst().orElseThrow();
        assertEquals(90.0, topCandidate.getTotalScore());
        assertEquals(true, topCandidate.getInstructorAvailable()); // 快照仍是可授课
        // 差异应能反映当前已变为不可用
        PlanSnapshotDiffResponse diff = planService.getDiff(draft.getId());
        assertTrue(diff.isHasAnyChange());
    }

    // ---------------- 双角色评审 ----------------

    @Test
    void requiresBothReviewsBeforePublish() {
        PlanCreateFromRecordRequest req = new PlanCreateFromRecordRequest();
        req.setMatchRecordId(recordId);
        Long id = planService.createDraft(req, "course_admin", "课程管理员").getId();
        planService.submit(id, "course_admin", "课程管理员");
        planService.reviewByInstructorAdmin(id, "APPROVED", "ok", "instructor_admin", "讲师管理员");

        // 只有一类通过时仍为评审中，不能发布
        assertEquals("IN_REVIEW", planService.getPlan(id).getStatus());
        assertThrows(IllegalArgumentException.class,
                () -> planService.publish(id, "course_admin", "课程管理员"));

        planService.reviewByCourseAdmin(id, "APPROVED", "ok", "course_admin", "课程管理员");
        assertEquals("APPROVED", planService.getPlan(id).getStatus());
        PlanResponse published = planService.publish(id, "course_admin", "课程管理员");
        assertEquals("PUBLISHED", published.getStatus());
        assertTrue(published.isCurrentActivePublished());
    }

    @Test
    void duplicateReviewRejectedWithoutCreatingSecondRecord() {
        Long id = createApprovedPlan();
        long reviewsBefore = reviewRepository.count();
        PlanConflictException ex = assertThrows(PlanConflictException.class,
                () -> planService.reviewByInstructorAdmin(id, "APPROVED", "重复点击",
                        "instructor_admin", "讲师管理员"));
        assertTrue(ex.getMessage().contains("已提交过"));
        assertEquals(reviewsBefore, reviewRepository.count(), "重复评审不得产生第二条记录");
    }

    @Test
    void adminCanRejectButCannotReplaceMissingProfessionalReview() {
        PlanCreateFromRecordRequest req = new PlanCreateFromRecordRequest();
        req.setMatchRecordId(recordId);
        Long id = planService.createDraft(req, "course_admin", "课程管理员").getId();
        planService.submit(id, "course_admin", "课程管理员");
        planService.reject(id, "资料需补充", "admin", "系统管理员");
        assertEquals("REJECTED", planService.getPlan(id).getStatus());
        // 驳回不产生任何专业评审记录
        assertEquals(0, reviewRepository.findByPlanIdOrderByCreatedAtAsc(id).size());
        // 驳回后修改重新提交，旧意见（若有）清空
        planService.submit(id, "course_admin", "课程管理员");
        assertEquals("IN_REVIEW", planService.getPlan(id).getStatus());
    }

    // ---------------- 并发唯一发布 ----------------

    @Test
    void concurrentPublishesOfTwoVersionsYieldSingleActiveVersion() throws Exception {
        Long v1 = createApprovedPlan();
        planService.publish(v1, "course_admin", "课程管理员");

        // 新版本基于同一推演创建并走完评审
        Long v2 = createApprovedPlan();

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    planService.publish(v2, "course_admin", "课程管理员");
                    success.incrementAndGet();
                } catch (PlanConflictException e) {
                    conflict.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(1, success.get(), "只有一个发布请求成功");
        assertEquals(threads - 1, conflict.get() + other.get(), "其余请求得到明确冲突结果");

        // 数据库中只有一个有效发布版本
        PublishedPlanSlot slot = slotRepository.findById(courseId).orElseThrow();
        assertEquals(v2, slot.getActivePlanId());
        assertEquals("PUBLISHED", planRepository.findById(v2).orElseThrow().getStatus());
        assertEquals("SUPERSEDED", planRepository.findById(v1).orElseThrow().getStatus());
        long publishedCount = planRepository.findByCourseIdOrderByVersionNoDesc(courseId).stream()
                .filter(p -> "PUBLISHED".equals(p.getStatus())).count();
        assertEquals(1, publishedCount);

        // 旧版本不得再发起正式指派
        assertThrows(IllegalArgumentException.class,
                () -> planAssignmentService.assignFromPlan(v1, "course_admin", "课程管理员"));
        assertEquals(0, assignmentRepository.findAll().size(), "旧版本指派失败不得产生半成品");
    }

    // ---------------- 资料变更失效 ----------------

    @Test
    void instructorChangeInvalidatesUnpublishedPlanWithSource() {
        Long id = createApprovedPlan(); // APPROVED，未发布
        invalidationService.onInstructorChanged(topInstructorId, "张三",
                "可授课状态变更", List.of("可授课状态"));

        MatchPlan plan = planRepository.findById(id).orElseThrow();
        assertEquals("INVALIDATED", plan.getStatus());
        assertEquals("INSTRUCTOR", plan.getInvalidSourceType());
        assertNotNull(plan.getInvalidReason());
        // 失效后不能评审/发布
        assertThrows(IllegalArgumentException.class,
                () -> planService.publish(id, "course_admin", "课程管理员"));
        // 轨迹含失效事件
        assertTrue(eventRepository.findByPlanIdOrderByIdAsc(id).stream()
                .anyMatch(e -> "INVALIDATED".equals(e.getEventType())));
    }

    @Test
    void weightChangeInvalidatesPlan() {
        Long id = createApprovedPlan();
        invalidationService.onWeightConfigChanged(weightId, "权重被修改", List.of("专业资质权重"));
        assertEquals("INVALIDATED", planRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void publishedPlanSurvivesInvalidationButAssignmentRechecksCapacity() {
        Long id = createApprovedPlan();
        planService.publish(id, "course_admin", "课程管理员");

        // 发布后资料变化不影响已发布版本状态（保留快照）
        invalidationService.onInstructorChanged(topInstructorId, "张三",
                "可授课状态变更", List.of("可授课状态"));
        MatchPlan published = planRepository.findById(id).orElseThrow();
        assertEquals("PUBLISHED", published.getStatus());

        // 但入选讲师当前不可用 -> 正式指派失败、无半成品、不替换为第2名
        tx.executeWithoutResult(s -> {
            Instructor top = instructorRepository.findById(topInstructorId).orElseThrow();
            top.setAvailable(false);
            instructorRepository.save(top);
        });
        PlanAssignmentResultResponse result =
                planAssignmentService.assignFromPlan(id, "course_admin", "课程管理员");
        assertFalse(result.isSuccess());
        assertEquals(0, assignmentRepository.findAll().size());
        assertEquals(topInstructorId, result.getInstructorId(), "仍锁定第1名候选，不换排名靠后者");
        assertTrue(eventRepository.findByPlanIdOrderByIdAsc(id).stream()
                .anyMatch(e -> "ASSIGNMENT_FAILED".equals(e.getEventType())));
    }

    @Test
    void capacityFullBlocksAssignmentWithoutHalfBakedResult() {
        Long id = createApprovedPlan();
        planService.publish(id, "course_admin", "课程管理员");

        // 把入选讲师本月容量占满（4/4）
        tx.executeWithoutResult(s -> {
            Instructor top = instructorRepository.findById(topInstructorId).orElseThrow();
            top.setMaxCoursesPerMonth(0);
            instructorRepository.save(top);
        });

        PlanAssignmentResultResponse result =
                planAssignmentService.assignFromPlan(id, "course_admin", "课程管理员");
        assertFalse(result.isSuccess());
        assertTrue(result.getReason().contains("容量"));
        assertEquals(0, assignmentRepository.findAll().size());
        assertEquals(topInstructorId, result.getInstructorId(), "不得改用第2名");
    }

    @Test
    void courseChangeInvalidatesUnpublishedPlan() {
        Long id = createApprovedPlan();
        invalidationService.onCourseChanged(courseId, "课程要求被修改", List.of("资质要求"));
        MatchPlan plan = planRepository.findById(id).orElseThrow();
        assertEquals("INVALIDATED", plan.getStatus());
        assertEquals("COURSE", plan.getInvalidSourceType());
        // 失效后阻止继续评审（400 或 409 都表达“不能继续”）
        assertThrows(RuntimeException.class,
                () -> planService.reviewByCourseAdmin(id, "APPROVED", "ok",
                        "course_admin", "课程管理员"));
    }

    // ---------------- 指派成功 + 幂等 ----------------

    @Test
    void successfulAssignmentRecordsSourceAndIsIdempotent() {
        Long id = createApprovedPlan();
        planService.publish(id, "course_admin", "课程管理员");

        PlanAssignmentResultResponse result =
                planAssignmentService.assignFromPlan(id, "course_admin", "课程管理员");
        assertTrue(result.isSuccess());
        assertEquals(1, result.getCandidateRank());
        assertEquals(topInstructorId, result.getInstructorId());

        CourseInstructorAssignment assignment = assignmentRepository.findAll().get(0);
        assertEquals(id, assignment.getSourcePlanId());
        assertNotNull(assignment.getSourceVersionNo());
        assertEquals(1, assignment.getSourceCandidateRank());
        assertEquals("CONFIRMED", assignment.getStatus());

        // 重复点击/超时重试：明确冲突，不产生第二条指派
        assertThrows(PlanConflictException.class,
                () -> planAssignmentService.assignFromPlan(id, "course_admin", "课程管理员"));
        assertEquals(1, assignmentRepository.findAll().size());
    }
}
