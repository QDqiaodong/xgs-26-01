package com.example.coursematch.controller;

import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.entity.CourseInstructorAssignment;
import com.example.coursematch.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "讲师分配", description = "课程讲师分配管理接口")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @Operation(summary = "分配讲师", description = "将讲师分配到课程")
    public ApiResponse<CourseInstructorAssignment> assign(
            @RequestParam Long courseId,
            @RequestParam Long instructorId,
            @RequestParam(required = false) Double matchScore,
            @RequestParam(required = false) String remark) {
        return ApiResponse.success("分配成功", assignmentService.assign(courseId, instructorId, matchScore, remark));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "确认分配", description = "确认讲师分配")
    public ApiResponse<CourseInstructorAssignment> confirm(@PathVariable Long id) {
        return ApiResponse.success("确认成功", assignmentService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "取消分配", description = "取消讲师分配")
    public ApiResponse<CourseInstructorAssignment> cancel(@PathVariable Long id) {
        return ApiResponse.success("取消成功", assignmentService.cancel(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "完成分配", description = "标记分配完成")
    public ApiResponse<CourseInstructorAssignment> complete(@PathVariable Long id) {
        return ApiResponse.success("完成成功", assignmentService.complete(id));
    }

    @GetMapping
    @Operation(summary = "获取所有分配记录", description = "获取所有讲师分配记录")
    public ApiResponse<List<CourseInstructorAssignment>> getAllAssignments() {
        return ApiResponse.success(assignmentService.getAllAssignments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分配详情", description = "根据ID获取分配记录详情")
    public ApiResponse<CourseInstructorAssignment> getAssignmentById(@PathVariable Long id) {
        return ApiResponse.success(assignmentService.getAssignmentById(id));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "获取课程分配", description = "获取指定课程的讲师分配记录")
    public ApiResponse<List<CourseInstructorAssignment>> getAssignmentsByCourseId(@PathVariable Long courseId) {
        return ApiResponse.success(assignmentService.getAssignmentsByCourseId(courseId));
    }

    @GetMapping("/instructor/{instructorId}")
    @Operation(summary = "获取讲师分配", description = "获取指定讲师的分配记录")
    public ApiResponse<List<CourseInstructorAssignment>> getAssignmentsByInstructorId(@PathVariable Long instructorId) {
        return ApiResponse.success(assignmentService.getAssignmentsByInstructorId(instructorId));
    }

    @GetMapping("/instructor/{instructorId}/available")
    @Operation(summary = "检查讲师可用性", description = "检查讲师是否可用")
    public ApiResponse<Boolean> isInstructorAvailable(@PathVariable Long instructorId) {
        return ApiResponse.success(assignmentService.isInstructorAvailable(instructorId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分配记录", description = "删除指定分配记录")
    public ApiResponse<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ApiResponse.success("删除成功", null);
    }
}