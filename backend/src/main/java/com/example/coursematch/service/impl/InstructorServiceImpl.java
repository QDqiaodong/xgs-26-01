package com.example.coursematch.service.impl;

import com.example.coursematch.controller.InstructorController;
import com.example.coursematch.dto.request.InstructorCreateRequest;
import com.example.coursematch.dto.request.InstructorUpdateRequest;
import com.example.coursematch.entity.Instructor;
import com.example.coursematch.repository.InstructorRepository;
import com.example.coursematch.service.InstructorService;
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
public class InstructorServiceImpl implements InstructorService {

    private final InstructorRepository instructorRepository;
    private final OperationLogService operationLogService;
    private final PlanInvalidationService planInvalidationService;

    @Override
    public Instructor createInstructor(InstructorCreateRequest request) {
        Instructor instructor = Instructor.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .certificates(request.getCertificates())
                .teachingYears(request.getTeachingYears())
                .studentRating(request.getStudentRating())
                .expertiseAreas(request.getExpertiseAreas())
                .teachingStyle(request.getTeachingStyle())
                .resume(request.getResume())
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        
        Instructor saved = instructorRepository.save(instructor);
        operationLogService.log("CREATE", "INSTRUCTOR", saved.getId(), 
                String.format("创建讲师：%s", saved.getName()));
        return saved;
    }

    @Override
    @Transactional
    public Instructor updateInstructor(Long id, InstructorUpdateRequest request) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + id));

        List<String> changedFields = new ArrayList<>();
        if (request.getName() != null) { instructor.setName(request.getName()); changedFields.add("姓名"); }
        if (request.getPhone() != null) { instructor.setPhone(request.getPhone()); }
        if (request.getEmail() != null) { instructor.setEmail(request.getEmail()); }
        if (request.getCertificates() != null) { instructor.setCertificates(request.getCertificates()); changedFields.add("资质证书"); }
        if (request.getTeachingYears() != null) { instructor.setTeachingYears(request.getTeachingYears()); changedFields.add("授课年限"); }
        if (request.getStudentRating() != null) { instructor.setStudentRating(request.getStudentRating()); changedFields.add("学员评分"); }
        if (request.getExpertiseAreas() != null) { instructor.setExpertiseAreas(request.getExpertiseAreas()); changedFields.add("擅长领域"); }
        if (request.getTeachingStyle() != null) { instructor.setTeachingStyle(request.getTeachingStyle()); changedFields.add("授课风格"); }
        if (request.getResume() != null) { instructor.setResume(request.getResume()); }
        if (request.getStatus() != null) { instructor.setStatus(request.getStatus()); changedFields.add("讲师状态"); }
        if (request.getAvailable() != null) { instructor.setAvailable(request.getAvailable()); changedFields.add("可授课状态"); }
        instructor.setUpdatedBy("system");

        Instructor saved = instructorRepository.save(instructor);
        operationLogService.log("UPDATE", "INSTRUCTOR", saved.getId(),
                String.format("更新讲师：%s", saved.getName()));
        if (!changedFields.isEmpty()) {
            planInvalidationService.onInstructorChanged(id, saved.getName(),
                    "讲师档案被修改", changedFields);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteInstructor(Long id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + id));
        planInvalidationService.onInstructorChanged(id, instructor.getName(),
                "讲师被删除", List.of("讲师删除"));
        instructorRepository.deleteById(id);
        operationLogService.log("DELETE", "INSTRUCTOR", id, "删除讲师");
    }

    @Override
    public Optional<Instructor> getInstructorById(Long id) {
        return instructorRepository.findById(id);
    }

    @Override
    public List<Instructor> getAllInstructors() {
        return instructorRepository.findByStatus("ACTIVE");
    }

    @Override
    public Page<Instructor> getInstructors(Pageable pageable, String status, Boolean available) {
        return instructorRepository.findByStatusAndAvailableOptional(status, available, pageable);
    }

    @Override
    @Transactional
    public Instructor updateInstructorStatus(Long id, String status) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + id));

        List<String> validStatuses = Arrays.asList("ACTIVE", "INACTIVE");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("无效的状态值: " + status + "，有效状态: " + validStatuses);
        }

        String old = instructor.getStatus();
        instructor.setStatus(status);
        instructor.setUpdatedBy("system");

        Instructor saved = instructorRepository.save(instructor);
        operationLogService.log("UPDATE_STATUS", "INSTRUCTOR", saved.getId(),
                String.format("讲师状态变更：%s -> %s", saved.getName(), status));
        if (!status.equals(old)) {
            planInvalidationService.onInstructorChanged(id, saved.getName(),
                    "讲师授课状态变更", List.of("讲师状态：" + old + " -> " + status));
        }
        return saved;
    }

    @Override
    @Transactional
    public Instructor updateInstructorAvailability(Long id, Boolean available) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + id));

        Boolean old = instructor.getAvailable();
        instructor.setAvailable(available);
        instructor.setUpdatedBy("system");

        Instructor saved = instructorRepository.save(instructor);
        operationLogService.log("UPDATE_AVAILABILITY", "INSTRUCTOR", saved.getId(),
                String.format("讲师可用性变更：%s -> %s", saved.getName(), available));
        if (!available.equals(old)) {
            planInvalidationService.onInstructorChanged(id, saved.getName(),
                    "讲师可授课状态变更", List.of("可授课状态：" + (Boolean.TRUE.equals(old) ? "可授课" : "不可授课")
                            + " -> " + (Boolean.TRUE.equals(available) ? "可授课" : "不可授课")));
        }
        return saved;
    }

    @Override
    public InstructorController.InstructorStatsResponse getInstructorStats() {
        InstructorController.InstructorStatsResponse stats = new InstructorController.InstructorStatsResponse();
        stats.setTotal(instructorRepository.count());
        stats.setActive(instructorRepository.countByStatus("ACTIVE"));
        stats.setInactive(instructorRepository.countByStatus("INACTIVE"));
        stats.setAvailable(instructorRepository.countByAvailable(true));
        stats.setUnavailable(instructorRepository.countByAvailable(false));
        stats.setAvgTeachingYears(instructorRepository.avgTeachingYearsByActive());
        stats.setAvgRating(instructorRepository.avgStudentRatingByActive());
        return stats;
    }
}