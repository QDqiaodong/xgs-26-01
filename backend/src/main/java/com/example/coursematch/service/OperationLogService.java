package com.example.coursematch.service;

import com.example.coursematch.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OperationLogService {

    void log(String operationType, String targetType, Long targetId, String details);

    void log(String operationType, String targetType, Long targetId, String operator, String ipAddress, String details, String result);

    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    Page<OperationLog> findByTargetType(String targetType, Pageable pageable);

    Page<OperationLog> findByOperator(String operator, Pageable pageable);

    Page<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
}