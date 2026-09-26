package com.example.coursematch.service.impl;

import com.example.coursematch.dto.response.FieldDiff;
import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.service.PlanSnapshotService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSnapshotServiceImpl implements PlanSnapshotService {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final WeightConfigRepository weightConfigRepository;
    private final MatchPlanCandidateRepository candidateRepository;
    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final InstructorSkillRepository instructorSkillRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SNAPSHOT_DELETED = "（当前资料已删除）";

    @Override
    @Transactional(readOnly = true)
    public PlanSnapshotDiffResponse buildDiff(MatchPlan plan) {
        List<FieldDiff> courseDiffs = buildCourseDiffs(plan);
        List<FieldDiff> weightDiffs = buildWeightDiffs(plan);
        List<FieldDiff> instructorDiffs = buildInstructorDiffs(plan);
        boolean weightDeleted = plan.getWeightConfigId() != null
                && weightConfigRepository.findById(plan.getWeightConfigId()).isEmpty();

        boolean hasAnyChange = false;
        for (FieldDiff d : courseDiffs) {
            if (d.isChanged()) { hasAnyChange = true; break; }
        }
        if (!hasAnyChange) {
            for (FieldDiff d : weightDiffs) {
                if (d.isChanged()) { hasAnyChange = true; break; }
            }
        }
        if (!hasAnyChange) {
            for (FieldDiff d : instructorDiffs) {
                if (d.isChanged()) { hasAnyChange = true; break; }
            }
        }

        String blockReason = checkTopCandidateAssignable(plan);

        return PlanSnapshotDiffResponse.builder()
                .planId(plan.getId())
                .versionNo(plan.getVersionNo())
                .courseDiffs(courseDiffs)
                .weightDiffs(weightDiffs)
                .instructorDiffs(instructorDiffs)
                .weightConfigDeleted(weightDeleted)
                .hasAnyChange(hasAnyChange || weightDeleted)
                .assignmentBlocked(blockReason != null)
                .assignmentBlockReason(blockReason)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String checkTopCandidateAssignable(MatchPlan plan) {
        List<MatchPlanCandidate> candidates = candidateRepository.findByPlanIdOrderByRankNoAsc(plan.getId());
        Optional<MatchPlanCandidate> top = candidates.stream().filter(c -> c.getRankNo() == 1).findFirst();
        if (top.isEmpty()) {
            return "方案缺少排名第一的候选讲师，无法发起正式指派";
        }
        MatchPlanCandidate candidate = top.get();
        Instructor instructor = instructorRepository.findById(candidate.getInstructorId()).orElse(null);
        if (instructor == null) {
            return "入选讲师「" + candidate.getInstructorName() + "」已被删除，无法指派";
        }
        if (!"ACTIVE".equals(instructor.getStatus())) {
            return "入选讲师「" + instructor.getName() + "」当前状态为 " + instructor.getStatus() + "，不可授课";
        }
        if (!Boolean.TRUE.equals(instructor.getAvailable())) {
            return "入选讲师「" + instructor.getName() + "」当前标记为不可用，无法指派";
        }
        int used = currentMonthConfirmedCount(instructor.getId());
        int capacity = instructor.getMaxCoursesPerMonth() == null ? 0 : instructor.getMaxCoursesPerMonth();
        if (used >= capacity) {
            return "入选讲师「" + instructor.getName() + "」本月课程容量已满（" + used + "/" + capacity + "），无法指派";
        }
        return null;
    }

    /** 本月已确认/进行中指派数（CONFIRMED 且未结束）。 */
    private int currentMonthConfirmedCount(Long instructorId) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        return assignmentRepository.countByInstructorIdAndStatusAndEndDateAfter(
                instructorId, "CONFIRMED", monthStart);
    }

    private List<FieldDiff> buildCourseDiffs(MatchPlan plan) {
        List<FieldDiff> diffs = new ArrayList<>();
        Course current = courseRepository.findById(plan.getCourseId()).orElse(null);
        if (current == null) {
            diffs.add(diff("COURSE", plan.getCourseId(), plan.getCourseName(),
                    "course", "课程", "存在", SNAPSHOT_DELETED, true));
            return diffs;
        }
        diffs.add(diff("COURSE", current.getId(), current.getName(), "name", "课程名称",
                plan.getCourseName(), current.getName()));
        diffs.add(diff("COURSE", current.getId(), current.getName(), "industry", "所属行业",
                plan.getCourseIndustry(), current.getIndustry()));
        diffs.add(diff("COURSE", current.getId(), current.getName(), "difficulty", "难度",
                plan.getCourseDifficulty(), current.getDifficulty()));
        diffs.add(diff("COURSE", current.getId(), current.getName(), "requiredQualifications", "资质要求",
                plan.getCourseRequiredQualifications(), current.getRequiredQualifications()));
        diffs.add(diff("COURSE", current.getId(), current.getName(), "styleRequirements", "风格要求",
                plan.getCourseStyleRequirements(), current.getStyleRequirements()));
        diffs.add(diff("COURSE", current.getId(), current.getName(), "status", "课程状态",
                plan.getCourseStatus(), current.getStatus()));
        return diffs;
    }

    private List<FieldDiff> buildWeightDiffs(MatchPlan plan) {
        List<FieldDiff> diffs = new ArrayList<>();
        if (plan.getWeightConfigId() == null) {
            return diffs;
        }
        WeightConfig current = weightConfigRepository.findById(plan.getWeightConfigId()).orElse(null);
        String name = plan.getWeightConfigName();
        if (current == null) {
            diffs.add(diff("WEIGHT", plan.getWeightConfigId(), name,
                    "weightConfig", "权重配置", "存在", SNAPSHOT_DELETED, true));
            return diffs;
        }
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "teachingYearsWeight", "授课年限权重",
                pct(plan.getTeachingYearsWeight()), pct(current.getTeachingYearsWeight())));
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "qualificationWeight", "专业资质权重",
                pct(plan.getQualificationWeight()), pct(current.getQualificationWeight())));
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "ratingWeight", "学员口碑权重",
                pct(plan.getRatingWeight()), pct(current.getRatingWeight())));
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "expertiseWeight", "行业擅长权重",
                pct(plan.getExpertiseWeight()), pct(current.getExpertiseWeight())));
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "styleWeight", "授课风格权重",
                pct(plan.getStyleWeight()), pct(current.getStyleWeight())));
        diffs.add(diff("WEIGHT", current.getId(), current.getName(), "gapThreshold", "缺口阈值",
                str(plan.getGapThreshold()), str(current.getGapThreshold())));
        return diffs;
    }

    private List<FieldDiff> buildInstructorDiffs(MatchPlan plan) {
        List<FieldDiff> diffs = new ArrayList<>();
        List<MatchPlanCandidate> candidates = candidateRepository.findByPlanIdOrderByRankNoAsc(plan.getId());
        for (MatchPlanCandidate c : candidates) {
            Instructor current = instructorRepository.findById(c.getInstructorId()).orElse(null);
            if (current == null) {
                diffs.add(diff("INSTRUCTOR", c.getInstructorId(), c.getInstructorName(),
                        "instructor", "讲师", "存在", SNAPSHOT_DELETED, true));
                continue;
            }
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "name", "姓名",
                    c.getInstructorName(), current.getName()));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "status", "讲师状态",
                    c.getInstructorStatus(), current.getStatus()));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "available", "可授课状态",
                    boolText(c.getInstructorAvailable()), boolText(current.getAvailable())));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "teachingYears", "授课年限",
                    str(c.getTeachingYears()), str(current.getTeachingYears())));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "studentRating", "学员评分",
                    str(c.getStudentRating()), str(current.getStudentRating())));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "certificates", "资质证书",
                    c.getCertificates(), current.getCertificates()));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "expertiseAreas", "擅长领域",
                    c.getExpertiseAreas(), current.getExpertiseAreas()));
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "teachingStyle", "授课风格",
                    c.getTeachingStyle(), current.getTeachingStyle()));

            List<String> snapshotSkills = readSkills(c.getSkillsSnapshot());
            List<String> currentSkills = instructorSkillRepository.findByInstructorId(current.getId())
                    .stream().map(InstructorSkill::getSkillName).sorted().toList();
            if (!snapshotSkills.equals(currentSkills)) {
                diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "skills", "资质/技能清单",
                        String.join("、", snapshotSkills), String.join("、", currentSkills), true));
            }

            int used = currentMonthConfirmedCount(current.getId());
            int capacity = current.getMaxCoursesPerMonth() == null ? 0 : current.getMaxCoursesPerMonth();
            String snapshotCapacity = "上限" + c.getMaxCoursesPerMonth();
            String currentCapacity = used + "/" + capacity;
            boolean capacityChanged = !Integer.valueOf(capacity).equals(c.getMaxCoursesPerMonth());
            diffs.add(diff("INSTRUCTOR", current.getId(), current.getName(), "monthCapacity", "本月容量",
                    snapshotCapacity, currentCapacity, capacityChanged));
        }
        return diffs;
    }

    private List<String> readSkills(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {}).stream().sorted().toList();
        } catch (Exception e) {
            log.warn("Failed to parse skills snapshot: {}", json, e);
            return List.of();
        }
    }

    private FieldDiff diff(String targetType, Long targetId, String targetName,
                           String field, String label, String snapshotValue, String currentValue) {
        return diff(targetType, targetId, targetName, field, label, snapshotValue, currentValue,
                !safeEquals(snapshotValue, currentValue));
    }

    private FieldDiff diff(String targetType, Long targetId, String targetName,
                           String field, String label, String snapshotValue, String currentValue,
                           boolean changed) {
        return FieldDiff.builder()
                .targetType(targetType)
                .targetId(targetId)
                .targetName(targetName)
                .field(field)
                .fieldLabel(label)
                .snapshotValue(snapshotValue)
                .currentValue(currentValue)
                .changed(changed)
                .build();
    }

    private boolean safeEquals(String a, String b) {
        String aa = a == null ? "" : a.trim();
        String bb = b == null ? "" : b.trim();
        return aa.equals(bb);
    }

    private String pct(Double v) {
        if (v == null) return "-";
        return Math.round(v * 100) + "%";
    }

    private String str(Object o) {
        return o == null ? "-" : String.valueOf(o);
    }

    private String boolText(Boolean b) {
        if (b == null) return "-";
        return b ? "可授课" : "不可授课";
    }
}
