package com.example.coursematch.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 方案快照与当前资料的差异视图。
 * 仅用于展示与发布前复核：无论差异如何，方案内的评分与排序都不会被重算。
 */
@Data
@Builder
public class PlanSnapshotDiffResponse {

    private Long planId;
    private Integer versionNo;

    /** 课程要求差异 */
    private List<FieldDiff> courseDiffs;

    /** 权重配置差异（权重被删除时 deleted=true） */
    private List<FieldDiff> weightDiffs;

    /** 各候选讲师资料/资质/可用状态差异 */
    private List<FieldDiff> instructorDiffs;

    private boolean weightConfigDeleted;

    /** 是否存在任何变化 */
    private boolean hasAnyChange;

    /** 影响正式指派的硬变化：入选候选不可用/容量不足 */
    private boolean assignmentBlocked;

    private String assignmentBlockReason;
}
