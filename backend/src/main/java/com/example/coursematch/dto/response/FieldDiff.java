package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 候选快照与当前讲师资料的单项差异。
 */
@Data
@Builder
public class FieldDiff {
    /** 差异归属：COURSE（课程要求） / INSTRUCTOR（讲师资料） / WEIGHT（权重） */
    private String targetType;
    private Long targetId;
    private String targetName;
    private String field;
    private String fieldLabel;
    private String snapshotValue;
    private String currentValue;
    private boolean changed;
}
