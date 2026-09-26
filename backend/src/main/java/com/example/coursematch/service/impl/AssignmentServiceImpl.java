package com.example.coursematch.service.impl;

import com.example.coursematch.entity.Course;
import com.example.coursematch.entity.CourseInstructorAssignment;
import com.example.coursematch.entity.Instructor;
import com.example.coursematch.repository.CourseInstructorAssignmentRepository;
import com.example.coursematch.repository.CourseRepository;
import com.example.coursematch.repository.InstructorRepository;
import com.example.coursematch.service.AssignmentService;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.PlanInvalidationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final OperationLogService operationLogService;
    private final PlanInvalidationService planInvalidationService;

    @Override
    @Transactional
    public CourseInstructorAssignment assign(Long courseId, Long instructorId, Double matchScore, String remark) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + courseId));
        
        Instructor instructor = instructorRepository.findByIdForUpdate(instructorId)
                .orElseThrow(() -> new EntityNotFoundException("讲师不存在: " + instructorId));

        // 锁内用最新数据复核容量，避免两个并发分配同时通过检查
        if (!isInstructorAvailable(instructorId)) {
            throw new IllegalArgumentException("讲师本月课程已满，无法分配");
        }
        
        if (assignmentRepository.existsByCourseIdAndInstructorIdAndStatus(courseId, instructorId, "CONFIRMED")) {
            throw new IllegalArgumentException("该讲师已分配到该课程");
        }
        
        CourseInstructorAssignment assignment = CourseInstructorAssignment.builder()
                .courseId(courseId)
                .courseName(course.getName())
                .instructorId(instructorId)
                .instructorName(instructor.getName())
                .matchScore(matchScore)
                .remark(remark)
                .status("PENDING")
                .build();
        
        assignment = assignmentRepository.save(assignment);
        
        operationLogService.log("CREATE", "ASSIGNMENT", assignment.getId(), 
                String.format("分配讲师 %s 到课程 %s，匹配度：%.2f", instructor.getName(), course.getName(), matchScore));
        
        return assignment;
    }

    @Override
    @Transactional
    public CourseInstructorAssignment confirm(Long id) {
        CourseInstructorAssignment assignment = getAssignmentById(id);

        if (!"PENDING".equals(assignment.getStatus())) {
            throw new IllegalArgumentException("只能确认待审批状态的分配");
        }

        assignment.setStatus("CONFIRMED");
        assignment.setStartDate(LocalDate.now());

        Instructor instructor = instructorRepository.findById(assignment.getInstructorId()).orElse(null);
        if (instructor != null) {
            instructor.setCurrentMonthCourses(instructor.getCurrentMonthCourses() + 1);
            instructorRepository.save(instructor);

            // 确认新的进行中指派会占用讲师本月容量：以该讲师为入选候选的未发布方案失效
            int used = getCurrentMonthCourseCount(instructor.getId());
            int capacity = instructor.getMaxCoursesPerMonth() == null ? 0 : instructor.getMaxCoursesPerMonth();
            planInvalidationService.onInstructorCapacityChanged(instructor.getId(), instructor.getName(),
                    "新增确认指派 #" + id + "，本月已占用 " + used + "/" + capacity);
        }

        assignment = assignmentRepository.save(assignment);

        operationLogService.log("UPDATE", "ASSIGNMENT", id, "确认分配");

        return assignment;
    }

    @Override
    @Transactional
    public CourseInstructorAssignment cancel(Long id) {
        CourseInstructorAssignment assignment = getAssignmentById(id);
        
        if ("COMPLETED".equals(assignment.getStatus())) {
            throw new IllegalArgumentException("已完成的分配不能取消");
        }
        
        assignment.setStatus("CANCELLED");
        assignment = assignmentRepository.save(assignment);
        
        operationLogService.log("UPDATE", "ASSIGNMENT", id, "取消分配");
        
        return assignment;
    }

    @Override
    @Transactional
    public CourseInstructorAssignment complete(Long id) {
        CourseInstructorAssignment assignment = getAssignmentById(id);
        
        if (!"CONFIRMED".equals(assignment.getStatus())) {
            throw new IllegalArgumentException("只能完成已确认状态的分配");
        }
        
        assignment.setStatus("COMPLETED");
        assignment.setEndDate(LocalDate.now());
        assignment = assignmentRepository.save(assignment);
        
        operationLogService.log("UPDATE", "ASSIGNMENT", id, "完成分配");
        
        return assignment;
    }

    @Override
    public List<CourseInstructorAssignment> getAssignmentsByCourseId(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }

    @Override
    public List<CourseInstructorAssignment> getAssignmentsByInstructorId(Long instructorId) {
        return assignmentRepository.findByInstructorId(instructorId);
    }

    @Override
    public List<CourseInstructorAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @Override
    public CourseInstructorAssignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("分配记录不存在: " + id));
    }

    @Override
    @Transactional
    public void deleteAssignment(Long id) {
        CourseInstructorAssignment assignment = getAssignmentById(id);
        assignmentRepository.delete(assignment);
        
        operationLogService.log("DELETE", "ASSIGNMENT", id, "删除分配记录");
    }

    @Override
    public boolean isInstructorAvailable(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId).orElse(null);
        if (instructor == null) {
            return false;
        }
        
        if (!instructor.getAvailable()) {
            return false;
        }
        
        int currentCount = getCurrentMonthCourseCount(instructorId);
        return currentCount < instructor.getMaxCoursesPerMonth();
    }

    @Override
    public int getCurrentMonthCourseCount(Long instructorId) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return assignmentRepository.countByInstructorIdAndStatusAndEndDateAfter(instructorId, "CONFIRMED", startOfMonth);
    }
}