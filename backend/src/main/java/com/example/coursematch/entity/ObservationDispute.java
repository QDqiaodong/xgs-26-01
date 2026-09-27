package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 讲师在确认前提出的事实异议；必须由另一名督导处理，原观察人隔离。 */
@Entity
@Table(name = "observation_disputes", indexes = @Index(name = "idx_dispute_observation", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(name = "raised_by", nullable = false, length = 50)
    private String raisedBy;

    @Column(name = "raised_by_name", nullable = false, length = 50)
    private String raisedByName;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;

    /** OPEN / UPHELD / CORRECTED / REVOKED / CLOSED_WITHOUT_CHANGE */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "reviewer_user_id")
    private Long reviewerUserId;

    @Column(name = "reviewer_username", length = 50)
    private String reviewerUsername;

    @Column(name = "reviewer_name", length = 50)
    private String reviewerName;

    @Column(name = "review_comment", length = 2000)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        raisedAt = LocalDateTime.now();
        if (status == null) status = "OPEN";
    }
}
