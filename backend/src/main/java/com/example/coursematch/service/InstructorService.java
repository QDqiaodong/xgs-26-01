package com.example.coursematch.service;

import com.example.coursematch.controller.InstructorController;
import com.example.coursematch.dto.request.InstructorCreateRequest;
import com.example.coursematch.dto.request.InstructorUpdateRequest;
import com.example.coursematch.entity.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InstructorService {
    Instructor createInstructor(InstructorCreateRequest request);
    Instructor updateInstructor(Long id, InstructorUpdateRequest request);
    void deleteInstructor(Long id);
    Optional<Instructor> getInstructorById(Long id);
    List<Instructor> getAllInstructors();
    Page<Instructor> getInstructors(Pageable pageable, String status, Boolean available);
    Instructor updateInstructorStatus(Long id, String status);
    Instructor updateInstructorAvailability(Long id, Boolean available);
    InstructorController.InstructorStatsResponse getInstructorStats();
}