package com.example.coursematch.repository;

import com.example.coursematch.entity.InstructorSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructorSkillRepository extends JpaRepository<InstructorSkill, Long> {
    List<InstructorSkill> findByInstructorId(Long instructorId);
    List<InstructorSkill> findByInstructorIdAndSkillType(Long instructorId, String skillType);
    void deleteByInstructorId(Long instructorId);
    long countByInstructorId(Long instructorId);
}