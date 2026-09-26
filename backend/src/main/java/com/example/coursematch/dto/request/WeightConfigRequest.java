package com.example.coursematch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeightConfigRequest {
    private String name;
    private String description;
    private Double teachingYearsWeight;
    private Double qualificationWeight;
    private Double ratingWeight;
    private Double expertiseWeight;
    private Double styleWeight;
    private Double gapThreshold;
}