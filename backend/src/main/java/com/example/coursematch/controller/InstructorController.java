package com.example.coursematch.controller;

import com.example.coursematch.dto.request.InstructorCreateRequest;
import com.example.coursematch.dto.request.InstructorUpdateRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.PageResponse;
import com.example.coursematch.entity.Instructor;
import com.example.coursematch.service.InstructorService;
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
@RequestMapping("/api/instructors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "讲师管理", description = "讲师综合资质档案录入相关接口")
public class InstructorController {

    private final InstructorService instructorService;

    @GetMapping
    @Operation(summary = "分页查询讲师列表", description = "支持按状态筛选，分页返回讲师列表")
    public ApiResponse<PageResponse<Instructor>> getInstructors(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "是否可用") @RequestParam(required = false) Boolean available) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Instructor> instructorPage = instructorService.getInstructors(pageable, status, available);
        return ApiResponse.success(PageResponse.from(instructorPage));
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有讲师", description = "不分页返回所有讲师")
    public ApiResponse<List<Instructor>> getAllInstructors() {
        return ApiResponse.success(instructorService.getAllInstructors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询讲师详情", description = "根据讲师ID查询讲师详细信息")
    public ApiResponse<Instructor> getInstructorById(@Parameter(description = "讲师ID") @PathVariable Long id) {
        return instructorService.getInstructorById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "讲师不存在"));
    }

    @PostMapping
    @Operation(summary = "创建讲师", description = "创建新的讲师资质档案")
    public ApiResponse<Instructor> createInstructor(@Valid @RequestBody InstructorCreateRequest request) {
        return ApiResponse.success("创建成功", instructorService.createInstructor(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新讲师", description = "更新讲师信息")
    public ApiResponse<Instructor> updateInstructor(@Parameter(description = "讲师ID") @PathVariable Long id, 
                                                     @Valid @RequestBody InstructorUpdateRequest request) {
        return ApiResponse.success("更新成功", instructorService.updateInstructor(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新讲师状态", description = "更新讲师状态（ACTIVE/INACTIVE）")
    public ApiResponse<Instructor> updateInstructorStatus(@Parameter(description = "讲师ID") @PathVariable Long id,
                                                           @Parameter(description = "状态") @RequestParam String status) {
        return ApiResponse.success("状态更新成功", instructorService.updateInstructorStatus(id, status));
    }

    @PatchMapping("/{id}/available")
    @Operation(summary = "更新讲师可用性", description = "设置讲师是否可接单")
    public ApiResponse<Instructor> updateInstructorAvailability(@Parameter(description = "讲师ID") @PathVariable Long id,
                                                                @Parameter(description = "是否可用") @RequestParam Boolean available) {
        return ApiResponse.success("可用性更新成功", instructorService.updateInstructorAvailability(id, available));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除讲师", description = "删除指定讲师")
    public ApiResponse<Void> deleteInstructor(@Parameter(description = "讲师ID") @PathVariable Long id) {
        instructorService.deleteInstructor(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/stats")
    @Operation(summary = "讲师统计", description = "获取讲师数量统计信息")
    public ApiResponse<InstructorStatsResponse> getInstructorStats() {
        InstructorStatsResponse stats = instructorService.getInstructorStats();
        return ApiResponse.success(stats);
    }

    @lombok.Data
    public static class InstructorStatsResponse {
        private long total;
        private long active;
        private long inactive;
        private long available;
        private long unavailable;
        private Double avgTeachingYears;
        private Double avgRating;
    }
}