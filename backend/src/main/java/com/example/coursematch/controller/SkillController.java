package com.example.coursematch.controller;

import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.entity.CourseRequiredSkill;
import com.example.coursematch.entity.InstructorSkill;
import com.example.coursematch.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "技能管理", description = "讲师技能和课程必备技能管理接口")
public class SkillController {

    private final SkillService skillService;

    @GetMapping("/instructors/{instructorId}")
    @Operation(summary = "查询讲师技能", description = "获取指定讲师的所有技能")
    public ApiResponse<List<InstructorSkill>> getInstructorSkills(
            @Parameter(description = "讲师ID") @PathVariable Long instructorId) {
        return ApiResponse.success(skillService.getInstructorSkills(instructorId));
    }

    @PostMapping("/instructors/{instructorId}")
    @Operation(summary = "添加讲师技能", description = "为讲师添加技能")
    public ApiResponse<InstructorSkill> addInstructorSkill(
            @Parameter(description = "讲师ID") @PathVariable Long instructorId,
            @RequestBody Map<String, String> request) {
        String skillName = request.get("skillName");
        String skillType = request.getOrDefault("skillType", "CERTIFICATE");
        return ApiResponse.success("添加成功", skillService.addInstructorSkill(instructorId, skillName, skillType));
    }

    @DeleteMapping("/instructors/{instructorId}/{skillId}")
    @Operation(summary = "删除讲师技能", description = "删除讲师的指定技能")
    public ApiResponse<Void> removeInstructorSkill(
            @Parameter(description = "讲师ID") @PathVariable Long instructorId,
            @Parameter(description = "技能ID") @PathVariable Long skillId) {
        skillService.removeInstructorSkill(instructorId, skillId);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/instructors/{instructorId}/batch")
    @Operation(summary = "批量添加讲师技能", description = "批量为讲师添加技能")
    public ApiResponse<Void> batchAddInstructorSkills(
            @Parameter(description = "讲师ID") @PathVariable Long instructorId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> skillNames = (List<String>) request.get("skillNames");
        String skillType = (String) request.getOrDefault("skillType", "CERTIFICATE");
        skillService.addInstructorSkills(instructorId, skillNames, skillType);
        return ApiResponse.success("批量添加成功", null);
    }

    @GetMapping("/courses/{courseId}")
    @Operation(summary = "查询课程必备技能", description = "获取指定课程的所有必备技能")
    public ApiResponse<List<CourseRequiredSkill>> getCourseRequiredSkills(
            @Parameter(description = "课程ID") @PathVariable Long courseId) {
        return ApiResponse.success(skillService.getCourseRequiredSkills(courseId));
    }

    @PostMapping("/courses/{courseId}")
    @Operation(summary = "添加课程必备技能", description = "为课程添加必备技能")
    public ApiResponse<CourseRequiredSkill> addCourseRequiredSkill(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @RequestBody Map<String, Object> request) {
        String skillName = (String) request.get("skillName");
        String skillType = (String) request.getOrDefault("skillType", "QUALIFICATION");
        Boolean isMandatory = (Boolean) request.getOrDefault("isMandatory", true);
        return ApiResponse.success("添加成功", skillService.addCourseRequiredSkill(courseId, skillName, skillType, isMandatory));
    }

    @DeleteMapping("/courses/{courseId}/{skillId}")
    @Operation(summary = "删除课程必备技能", description = "删除课程的指定必备技能")
    public ApiResponse<Void> removeCourseRequiredSkill(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @Parameter(description = "技能ID") @PathVariable Long skillId) {
        skillService.removeCourseRequiredSkill(courseId, skillId);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/courses/{courseId}/batch")
    @Operation(summary = "批量添加课程必备技能", description = "批量为课程添加必备技能")
    public ApiResponse<Void> batchAddCourseRequiredSkills(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> skillNames = (List<String>) request.get("skillNames");
        String skillType = (String) request.getOrDefault("skillType", "QUALIFICATION");
        Boolean isMandatory = (Boolean) request.getOrDefault("isMandatory", true);
        skillService.addCourseRequiredSkills(courseId, skillNames, skillType, isMandatory);
        return ApiResponse.success("批量添加成功", null);
    }

    @GetMapping("/match/{courseId}/{instructorId}")
    @Operation(summary = "计算技能匹配度", description = "计算讲师与课程的技能匹配分数")
    public ApiResponse<Map<String, Double>> calculateSkillMatchScore(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @Parameter(description = "讲师ID") @PathVariable Long instructorId) {
        double score = skillService.calculateSkillMatchScore(courseId, instructorId);
        return ApiResponse.success(Map.of("matchScore", score));
    }
}