package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 草稿阶段调整候选讲师集合。提交的讲师 ID 集合按来源推演的得分重新固化排序；
 * 仅 DRAFT / REJECTED（重新提交前）允许修改，提交评审后候选锁定。
 */
@Data
public class PlanUpdateCandidatesRequest {

    @NotEmpty(message = "候选讲师不能为空")
    private List<Long> instructorIds;
}
