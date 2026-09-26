package com.example.coursematch.repository;

import com.example.coursematch.entity.CourseRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRequiredSkillRepository extends JpaRepository<CourseRequiredSkill, Long> {
    List<CourseRequiredSkill> findByCourseId(Long courseId);
    List<CourseRequiredSkill> findByCourseIdAndIsMandatory(Long courseId, Boolean isMandatory);
    void deleteByCourseId(Long courseId);
    long countByCourseId(Long courseId);
}