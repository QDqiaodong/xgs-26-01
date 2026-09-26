package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_record_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRecordDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "instructor_name", length = 50)
    private String instructorName;

    @Column(name = "total_score", nullable = false)
    private Double totalScore;

    @Column(name = "teaching_years_score")
    private Double teachingYearsScore;

    @Column(name = "qualification_score")
    private Double qualificationScore;

    @Column(name = "rating_score")
    private Double ratingScore;

    @Column(name = "expertise_score")
    private Double expertiseScore;

    @Column(name = "style_score")
    private Double styleScore;

    @Column(name = "match_rank", nullable = false)
    private Integer rank;
}