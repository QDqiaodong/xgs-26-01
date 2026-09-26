package com.example.coursematch.service.impl;

import com.example.coursematch.entity.OperationLog;
import com.example.coursematch.repository.OperationLogRepository;
import com.example.coursematch.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Async
    @Override
    public void log(String operationType, String targetType, Long targetId, String details) {
        log(operationType, targetType, targetId, "system", null, details, "SUCCESS");
    }

    @Async
    @Override
    public void log(String operationType, String targetType, Long targetId, String operator, String ipAddress, String details, String result) {
        try {
            OperationLog operationLog = OperationLog.builder()
                    .operationType(operationType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .operator(operator)
                    .ipAddress(ipAddress)
                    .operationTime(LocalDateTime.now())
                    .details(details)
                    .result(result)
                    .build();
            operationLogRepository.save(operationLog);
        } catch (Exception e) {
            log.error("Failed to save operation log: {}", e.getMessage());
        }
    }

    @Override
    public Page<OperationLog> findByOperationType(String operationType, Pageable pageable) {
        return operationLogRepository.findByOperationType(operationType, pageable);
    }

    @Override
    public Page<OperationLog> findByTargetType(String targetType, Pageable pageable) {
        return operationLogRepository.findByTargetType(targetType, pageable);
    }

    @Override
    public Page<OperationLog> findByOperator(String operator, Pageable pageable) {
        return operationLogRepository.findByOperator(operator, pageable);
    }

    @Override
    public Page<OperationLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return operationLogRepository.findByTimeRange(startTime, endTime, pageable);
    }
}