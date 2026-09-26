package com.example.coursematch.repository;

import com.example.coursematch.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    Page<OperationLog> findByTargetType(String targetType, Pageable pageable);

    Page<OperationLog> findByOperator(String operator, Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE o.operationTime BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime, 
                                        Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE o.targetType = :targetType AND o.targetId = :targetId")
    List<OperationLog> findByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
}