package com.example.coursematch.service.impl;

import com.example.coursematch.entity.CourseRequiredSkill;
import com.example.coursematch.entity.InstructorSkill;
import com.example.coursematch.repository.CourseRequiredSkillRepository;
import com.example.coursematch.repository.InstructorSkillRepository;
import com.example.coursematch.service.PlanInvalidationService;
import com.example.coursematch.service.SkillService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final InstructorSkillRepository instructorSkillRepository;
    private final CourseRequiredSkillRepository courseRequiredSkillRepository;
    private final PlanInvalidationService planInvalidationService;

    @Override
    @Transactional
    public InstructorSkill addInstructorSkill(Long instructorId, String skillName, String skillType) {
        InstructorSkill skill = InstructorSkill.builder()
                .instructorId(instructorId)
                .skillName(skillName.trim())
                .skillType(skillType != null ? skillType : "CERTIFICATE")
                .build();
        InstructorSkill saved = instructorSkillRepository.save(skill);
        planInvalidationService.onInstructorChanged(instructorId, null,
                "新增资质/技能「" + saved.getSkillName() + "」", List.of("资质技能清单"));
        return saved;
    }

    @Override
    @Transactional
    public void removeInstructorSkill(Long instructorId, Long skillId) {
        InstructorSkill skill = instructorSkillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("技能不存在: " + skillId));
        if (!skill.getInstructorId().equals(instructorId)) {
            throw new IllegalArgumentException("技能不属于该讲师");
        }
        instructorSkillRepository.delete(skill);
        planInvalidationService.onInstructorChanged(instructorId, null,
                "删除资质/技能「" + skill.getSkillName() + "」", List.of("资质技能清单"));
    }

    @Override
    public List<InstructorSkill> getInstructorSkills(Long instructorId) {
        return instructorSkillRepository.findByInstructorId(instructorId);
    }

    @Override
    public void addInstructorSkills(Long instructorId, List<String> skillNames, String skillType) {
        for (String skillName : skillNames) {
            if (skillName != null && !skillName.trim().isEmpty()) {
                addInstructorSkill(instructorId, skillName.trim(), skillType);
            }
        }
    }

    @Override
    @Transactional
    public CourseRequiredSkill addCourseRequiredSkill(Long courseId, String skillName, String skillType, Boolean isMandatory) {
        CourseRequiredSkill skill = CourseRequiredSkill.builder()
                .courseId(courseId)
                .skillName(skillName.trim())
                .skillType(skillType != null ? skillType : "QUALIFICATION")
                .isMandatory(isMandatory != null ? isMandatory : true)
                .build();
        CourseRequiredSkill saved = courseRequiredSkillRepository.save(skill);
        planInvalidationService.onCourseChanged(courseId,
                "新增必备资质「" + saved.getSkillName() + "」", List.of("必备资质清单"));
        return saved;
    }

    @Override
    @Transactional
    public void removeCourseRequiredSkill(Long courseId, Long skillId) {
        CourseRequiredSkill skill = courseRequiredSkillRepository.findById(skillId)
                .orElseThrow(() -> new EntityNotFoundException("必备技能不存在: " + skillId));
        if (!skill.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("必备技能不属于该课程");
        }
        courseRequiredSkillRepository.delete(skill);
        planInvalidationService.onCourseChanged(courseId,
                "删除必备资质「" + skill.getSkillName() + "」", List.of("必备资质清单"));
    }

    @Override
    public List<CourseRequiredSkill> getCourseRequiredSkills(Long courseId) {
        return courseRequiredSkillRepository.findByCourseId(courseId);
    }

    @Override
    public void addCourseRequiredSkills(Long courseId, List<String> skillNames, String skillType, Boolean isMandatory) {
        for (String skillName : skillNames) {
            if (skillName != null && !skillName.trim().isEmpty()) {
                addCourseRequiredSkill(courseId, skillName.trim(), skillType, isMandatory);
            }
        }
    }

    @Override
    public double calculateSkillMatchScore(Long courseId, Long instructorId) {
        List<CourseRequiredSkill> requiredSkills = courseRequiredSkillRepository.findByCourseId(courseId);
        List<InstructorSkill> instructorSkills = instructorSkillRepository.findByInstructorId(instructorId);

        if (requiredSkills.isEmpty()) {
            return 30.0;
        }

        Set<String> instructorSkillNames = instructorSkills.stream()
                .map(s -> s.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        int mandatoryMatched = 0;
        int mandatoryTotal = 0;
        int optionalMatched = 0;
        int optionalTotal = 0;

        for (CourseRequiredSkill reqSkill : requiredSkills) {
            String reqSkillName = reqSkill.getSkillName().toLowerCase();
            boolean matched = instructorSkillNames.contains(reqSkillName);
            
            if (reqSkill.getIsMandatory()) {
                mandatoryTotal++;
                if (matched) mandatoryMatched++;
            } else {
                optionalTotal++;
                if (matched) optionalMatched++;
            }
        }

        double mandatoryScore = mandatoryTotal > 0 ? (double) mandatoryMatched / mandatoryTotal * 100 : 0;
        double optionalScore = optionalTotal > 0 ? (double) optionalMatched / optionalTotal * 100 : 0;

        return (mandatoryScore * 0.7 + optionalScore * 0.3);
    }
}