package com.example.coursematch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultResponse {
    private Long recordId;
    private String courseName;
    private String courseIndustry;
    private Boolean hasGap;
    private Double bestScore;
    private Integer matchCount;
    private List<InstructorMatchDetail> instructors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorMatchDetail {
        private Long instructorId;
        private String instructorName;
        private Double totalScore;
        private Double teachingYearsScore;
        private Double qualificationScore;
        private Double ratingScore;
        private Double expertiseScore;
        private Double styleScore;
        private Integer rank;
        private Boolean available;
    }
}