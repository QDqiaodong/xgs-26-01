package com.example.coursematch.service.impl;

import com.example.coursematch.controller.CourseController;
import com.example.coursematch.dto.request.CourseCreateRequest;
import com.example.coursematch.dto.request.CourseUpdateRequest;
import com.example.coursematch.entity.Course;
import com.example.coursematch.repository.CourseRepository;
import com.example.coursematch.service.CourseService;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.PlanInvalidationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final OperationLogService operationLogService;
    private final PlanInvalidationService planInvalidationService;

    @Override
    public Course createCourse(CourseCreateRequest request) {
        Course course = Course.builder()
                .name(request.getName())
                .industry(request.getIndustry())
                .difficulty(request.getDifficulty())
                .requiredQualifications(request.getRequiredQualifications())
                .styleRequirements(request.getStyleRequirements())
                .description(request.getDescription())
                .totalHours(request.getTotalHours())
                .maxStudents(request.getMaxStudents())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .createdBy("system")
                .build();
        
        Course saved = courseRepository.save(course);
        operationLogService.log("CREATE", "COURSE", saved.getId(), 
                String.format("创建课程：%s", saved.getName()));
        return saved;
    }

    @Override
    @Transactional
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + id));

        List<String> changedFields = new ArrayList<>();
        if (request.getName() != null) { course.setName(request.getName()); changedFields.add("课程名称"); }
        if (request.getIndustry() != null) { course.setIndustry(request.getIndustry()); changedFields.add("所属行业"); }
        if (request.getDifficulty() != null) { course.setDifficulty(request.getDifficulty()); changedFields.add("难度"); }
        if (request.getRequiredQualifications() != null) { course.setRequiredQualifications(request.getRequiredQualifications()); changedFields.add("资质要求"); }
        if (request.getStyleRequirements() != null) { course.setStyleRequirements(request.getStyleRequirements()); changedFields.add("风格要求"); }
        if (request.getDescription() != null) { course.setDescription(request.getDescription()); changedFields.add("课程描述"); }
        if (request.getTotalHours() != null) { course.setTotalHours(request.getTotalHours()); changedFields.add("总课时"); }
        if (request.getMaxStudents() != null) { course.setMaxStudents(request.getMaxStudents()); changedFields.add("学员上限"); }
        if (request.getStatus() != null) { course.setStatus(request.getStatus()); changedFields.add("课程状态"); }
        course.setUpdatedBy("system");

        Course saved = courseRepository.save(course);
        operationLogService.log("UPDATE", "COURSE", saved.getId(),
                String.format("更新课程：%s", saved.getName()));
        if (!changedFields.isEmpty()) {
            // 课程要求变化 -> 基于该课程的未发布方案失效
            planInvalidationService.onCourseChanged(id, "课程「" + saved.getName() + "」被修改", changedFields);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + id));
        // 课程删除 -> 未发布方案失效（已发布方案保留快照）
        planInvalidationService.onCourseChanged(id, "课程「" + course.getName() + "」被删除",
                List.of("课程删除"));
        courseRepository.deleteById(id);
        operationLogService.log("DELETE", "COURSE", id, "删除课程");
    }

    @Override
    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findByStatus("ACTIVE");
    }

    @Override
    public List<Course> getCoursesByIndustry(String industry) {
        return courseRepository.findByIndustry(industry);
    }

    @Override
    public Page<Course> getCourses(Pageable pageable, String industry, String status) {
        return courseRepository.findByIndustryAndStatusOptional(industry, status, pageable);
    }

    @Override
    @Transactional
    public Course updateCourseStatus(Long id, String status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + id));

        List<String> validStatuses = Arrays.asList("DRAFT", "ACTIVE", "INACTIVE");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("无效的状态值: " + status + "，有效状态: " + validStatuses);
        }

        String oldStatus = course.getStatus();
        course.setStatus(status);
        course.setUpdatedBy("system");

        Course saved = courseRepository.save(course);
        operationLogService.log("UPDATE_STATUS", "COURSE", saved.getId(),
                String.format("课程状态变更：%s -> %s", saved.getName(), status));
        if (!status.equals(oldStatus)) {
            planInvalidationService.onCourseChanged(id,
                    "课程「" + saved.getName() + "」状态变更", List.of("课程状态：" + oldStatus + " -> " + status));
        }
        return saved;
    }

    @Override
    public CourseController.CourseStatsResponse getCourseStats() {
        CourseController.CourseStatsResponse stats = new CourseController.CourseStatsResponse();
        stats.setTotal(courseRepository.count());
        stats.setActive(courseRepository.countByStatus("ACTIVE"));
        stats.setDraft(courseRepository.countByStatus("DRAFT"));
        stats.setInactive(courseRepository.countByStatus("INACTIVE"));
        
        List<Object[]> industryCounts = courseRepository.countByIndustry();
        List<CourseController.CourseStatsResponse.IndustryStats> industryStats = new ArrayList<>();
        for (Object[] row : industryCounts) {
            CourseController.CourseStatsResponse.IndustryStats is = new CourseController.CourseStatsResponse.IndustryStats();
            is.setIndustry((String) row[0]);
            is.setCount((Long) row[1]);
            industryStats.add(is);
        }
        stats.setIndustryStats(industryStats);
        
        return stats;
    }
}