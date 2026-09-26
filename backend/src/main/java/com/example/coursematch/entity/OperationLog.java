package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "operator", length = 50)
    private String operator;

    @Column(name = "operation_time", nullable = false)
    private LocalDateTime operationTime;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "result", length = 20)
    private String result;

    @PrePersist
    protected void onCreate() {
        if (operationTime == null) {
            operationTime = LocalDateTime.now();
        }
    }
}