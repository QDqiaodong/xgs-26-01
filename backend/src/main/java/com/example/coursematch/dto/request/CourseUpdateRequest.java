package com.example.coursematch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdateRequest {
    private String name;
    private String industry;
    private String difficulty;
    private String requiredQualifications;
    private String styleRequirements;
    private String description;
    private Boolean active;
    private Integer totalHours;
    private Integer maxStudents;
    private String status;
}