package com.example.coursematch.controller;

import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.PageResponse;
import com.example.coursematch.entity.OperationLog;
import com.example.coursematch.repository.OperationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "操作日志", description = "系统操作日志查询接口")
public class LogController {

    private final OperationLogRepository operationLogRepository;

    @GetMapping
    @Operation(summary = "分页查询操作日志", description = "支持按操作类型、目标类型、操作人、时间范围筛选")
    public ApiResponse<PageResponse<OperationLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operationTime"));
        Page<OperationLog> logPage;
        
        if (startDate != null && endDate != null) {
            LocalDateTime startTime = startDate.atStartOfDay();
            LocalDateTime endTime = endDate.atTime(LocalTime.MAX);
            logPage = operationLogRepository.findByTimeRange(startTime, endTime, pageable);
        } else if (operationType != null) {
            logPage = operationLogRepository.findByOperationType(operationType, pageable);
        } else if (targetType != null) {
            logPage = operationLogRepository.findByTargetType(targetType, pageable);
        } else if (operator != null) {
            logPage = operationLogRepository.findByOperator(operator, pageable);
        } else {
            logPage = operationLogRepository.findAll(pageable);
        }
        
        return ApiResponse.success(PageResponse.from(logPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询日志详情", description = "根据日志ID查询详细信息")
    public ApiResponse<OperationLog> getLogById(@PathVariable Long id) {
        return operationLogRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "日志不存在"));
    }
}