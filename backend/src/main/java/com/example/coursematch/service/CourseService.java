package com.example.coursematch.service;

import com.example.coursematch.controller.CourseController;
import com.example.coursematch.dto.request.CourseCreateRequest;
import com.example.coursematch.dto.request.CourseUpdateRequest;
import com.example.coursematch.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    Course createCourse(CourseCreateRequest request);
    Course updateCourse(Long id, CourseUpdateRequest request);
    void deleteCourse(Long id);
    Optional<Course> getCourseById(Long id);
    List<Course> getAllCourses();
    List<Course> getCoursesByIndustry(String industry);
    Page<Course> getCourses(Pageable pageable, String industry, String status);
    Course updateCourseStatus(Long id, String status);
    CourseController.CourseStatsResponse getCourseStats();
}