package com.example.coursematch.repository;

import com.example.coursematch.entity.CourseInstructorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseInstructorAssignmentRepository extends JpaRepository<CourseInstructorAssignment, Long> {

    List<CourseInstructorAssignment> findByCourseId(Long courseId);

    List<CourseInstructorAssignment> findByInstructorId(Long instructorId);

    List<CourseInstructorAssignment> findByStatus(String status);

    List<CourseInstructorAssignment> findByInstructorIdAndStatus(Long instructorId, String status);

    List<CourseInstructorAssignment> findByInstructorIdAndStatusAndEndDateAfter(Long instructorId, String status, LocalDate date);

    boolean existsByCourseIdAndInstructorIdAndStatus(Long courseId, Long instructorId, String status);

    int countByInstructorIdAndStatusAndEndDateAfter(Long instructorId, String status, LocalDate date);
}