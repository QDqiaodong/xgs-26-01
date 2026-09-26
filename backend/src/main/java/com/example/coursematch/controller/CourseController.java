package com.example.coursematch.controller;

import com.example.coursematch.dto.request.CourseCreateRequest;
import com.example.coursematch.dto.request.CourseUpdateRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.PageResponse;
import com.example.coursematch.entity.Course;
import com.example.coursematch.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "课程管理", description = "课程需求档案配置相关接口")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "分页查询课程列表", description = "支持按行业、状态筛选，分页返回课程列表")
    public ApiResponse<PageResponse<Course>> getCourses(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "行业筛选") @RequestParam(required = false) String industry,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Course> coursePage = courseService.getCourses(pageable, industry, status);
        return ApiResponse.success(PageResponse.from(coursePage));
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有课程", description = "不分页返回所有课程")
    public ApiResponse<List<Course>> getAllCourses() {
        return ApiResponse.success(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询课程详情", description = "根据课程ID查询课程详细信息")
    public ApiResponse<Course> getCourseById(@Parameter(description = "课程ID") @PathVariable Long id) {
        return courseService.getCourseById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "课程不存在"));
    }

    @GetMapping("/industry/{industry}")
    @Operation(summary = "按行业查询课程", description = "查询指定行业的所有课程")
    public ApiResponse<List<Course>> getCoursesByIndustry(@Parameter(description = "行业名称") @PathVariable String industry) {
        return ApiResponse.success(courseService.getCoursesByIndustry(industry));
    }

    @PostMapping
    @Operation(summary = "创建课程", description = "创建新的课程需求档案")
    public ApiResponse<Course> createCourse(@Valid @RequestBody CourseCreateRequest request) {
        return ApiResponse.success("创建成功", courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新课程", description = "更新课程信息")
    public ApiResponse<Course> updateCourse(@Parameter(description = "课程ID") @PathVariable Long id, 
                                            @Valid @RequestBody CourseUpdateRequest request) {
        return ApiResponse.success("更新成功", courseService.updateCourse(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新课程状态", description = "更新课程状态（DRAFT/ACTIVE/INACTIVE）")
    public ApiResponse<Course> updateCourseStatus(@Parameter(description = "课程ID") @PathVariable Long id,
                                                   @Parameter(description = "状态") @RequestParam String status) {
        return ApiResponse.success("状态更新成功", courseService.updateCourseStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除课程", description = "删除指定课程")
    public ApiResponse<Void> deleteCourse(@Parameter(description = "课程ID") @PathVariable Long id) {
        courseService.deleteCourse(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/stats")
    @Operation(summary = "课程统计", description = "获取课程数量统计信息")
    public ApiResponse<CourseStatsResponse> getCourseStats() {
        CourseStatsResponse stats = courseService.getCourseStats();
        return ApiResponse.success(stats);
    }

    @lombok.Data
    public static class CourseStatsResponse {
        private long total;
        private long active;
        private long draft;
        private long inactive;
        private List<IndustryStats> industryStats;

        @lombok.Data
        public static class IndustryStats {
            private String industry;
            private long count;
        }
    }
}