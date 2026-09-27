package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 事后补充只能追加，不能覆盖观察项原始记录。 */
@Entity
@Table(name = "observation_addenda")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationAddendum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "sensitivity_level", nullable = false, length = 30)
    private String sensitivityLevel;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "author_username", nullable = false, length = 50)
    private String authorUsername;

    @Column(name = "author_name", nullable = false, length = 50)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
