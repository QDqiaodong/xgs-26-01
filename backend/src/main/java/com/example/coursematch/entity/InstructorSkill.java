package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "instructor_skills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(nullable = false, length = 100)
    private String skillName;

    @Column(length = 50)
    private String skillType;

    @Column(length = 200)
    private String description;

    @Column(name = "acquired_date")
    private LocalDateTime acquiredDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}