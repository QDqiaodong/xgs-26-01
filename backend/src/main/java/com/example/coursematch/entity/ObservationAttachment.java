package com.example.coursematch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观察附件来源登记（录像、听课记录、问卷、截图等）。
 * 附件本身按对象存储/文件服务管理，这里固化来源说明与分级，保证每次观察可溯源。
 */
@Entity
@Table(name = "observation_attachments",
        indexes = @Index(name = "idx_obs_att_obs", columnList = "observation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "observation_id", nullable = false)
    private Long observationId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 文件标识/相对路径 */
    @Column(name = "file_ref", length = 500)
    private String fileRef;

    @Column(name = "source_note", length = 500)
    private String sourceNote;

    /** 内容分级：INTERNAL（如投诉原文录音）默认不对讲师开放 */
    @Column(name = "sensitivity_level", nullable = false, length = 25)
    @Builder.Default
    private String sensitivityLevel = "INTERNAL";

    @Column(name = "uploaded_by", nullable = false, length = 50)
    private String uploadedBy;

    @Column(name = "uploaded_by_name", length = 50)
    private String uploadedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
