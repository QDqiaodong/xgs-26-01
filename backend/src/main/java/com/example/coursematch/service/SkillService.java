package com.example.coursematch.service;

import com.example.coursematch.entity.CourseRequiredSkill;
import com.example.coursematch.entity.InstructorSkill;

import java.util.List;

public interface SkillService {

    InstructorSkill addInstructorSkill(Long instructorId, String skillName, String skillType);
    
    void removeInstructorSkill(Long instructorId, Long skillId);
    
    List<InstructorSkill> getInstructorSkills(Long instructorId);
    
    void addInstructorSkills(Long instructorId, List<String> skillNames, String skillType);

    CourseRequiredSkill addCourseRequiredSkill(Long courseId, String skillName, String skillType, Boolean isMandatory);
    
    void removeCourseRequiredSkill(Long courseId, Long skillId);
    
    List<CourseRequiredSkill> getCourseRequiredSkills(Long courseId);
    
    void addCourseRequiredSkills(Long courseId, List<String> skillNames, String skillType, Boolean isMandatory);
    
    double calculateSkillMatchScore(Long courseId, Long instructorId);
}