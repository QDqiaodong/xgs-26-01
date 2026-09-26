package com.example.coursematch.service;

import com.example.coursematch.entity.CourseInstructorAssignment;

import java.util.List;

public interface AssignmentService {
    
    CourseInstructorAssignment assign(Long courseId, Long instructorId, Double matchScore, String remark);
    
    CourseInstructorAssignment confirm(Long id);
    
    CourseInstructorAssignment cancel(Long id);
    
    CourseInstructorAssignment complete(Long id);
    
    List<CourseInstructorAssignment> getAssignmentsByCourseId(Long courseId);
    
    List<CourseInstructorAssignment> getAssignmentsByInstructorId(Long instructorId);
    
    List<CourseInstructorAssignment> getAllAssignments();
    
    CourseInstructorAssignment getAssignmentById(Long id);
    
    void deleteAssignment(Long id);
    
    boolean isInstructorAvailable(Long instructorId);
    
    int getCurrentMonthCourseCount(Long instructorId);
}