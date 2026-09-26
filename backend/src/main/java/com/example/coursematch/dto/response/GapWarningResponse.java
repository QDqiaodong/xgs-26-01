package com.example.coursematch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GapWarningResponse {
    private Long courseId;
    private String courseName;
    private String industry;
    private String difficulty;
    private Double bestScore;
    private Integer matchCount;
    private LocalDateTime lastMatchTime;
    private String warningLevel;
}