package com.example.coursematch.controller;

import com.example.coursematch.dto.request.PlanCreateFromRecordRequest;
import com.example.coursematch.dto.request.PlanRejectRequest;
import com.example.coursematch.dto.request.PlanReviewRequest;
import com.example.coursematch.dto.request.PlanUpdateCandidatesRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.PlanAssignmentResultResponse;
import com.example.coursematch.dto.response.PlanResponse;
import com.example.coursematch.dto.response.PlanSnapshotDiffResponse;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.CurrentUserProvider;
import com.example.coursematch.service.MatchPlanService;
import com.example.coursematch.service.PlanAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推演方案“评审发布”链路。
 *
 * <p>角色边界（服务端强制，前端隐藏按钮仅为体验）：
 * <ul>
 *   <li>课程管理员 COURSE_ADMIN：保存草稿、调整候选、提交评审、课程侧评审、发布、正式指派</li>
 *   <li>讲师管理员 INSTRUCTOR_ADMIN：仅讲师侧评审（资质/可授课状态）</li>
 *   <li>系统管理员 ADMIN：可驳回、可查看，但不能提交专业评审、不能发布、不能指派</li>
 *   <li>查看者 VIEWER（及任何已登录用户）：只读方案、差异、评审轨迹，不能提交意见或改变状态</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "推演方案评审发布", description = "推演结果保存为带版本方案、双角色评审、唯一发布、正式指派")
public class MatchPlanController {

    private final MatchPlanService planService;
    private final PlanAssignmentService planAssignmentService;

    private static final String COURSE_ADMIN = "COURSE_ADMIN";
    private static final String INSTRUCTOR_ADMIN = "INSTRUCTOR_ADMIN";
    private static final String ADMIN = "ADMIN";

    @PostMapping
    @Operation(summary = "保存为草稿方案", description = "课程管理员把某次推演结果固化为带版本草稿（课程要求/权重/候选资料/五项评分/排序快照）")
    public ApiResponse<PlanResponse> createDraft(@Valid @RequestBody PlanCreateFromRecordRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        return ApiResponse.success("草稿已创建，快照已固化",
                planService.createDraft(request, user.getUsername(), user.getRealName()));
    }

    @PutMapping("/{id}/candidates")
    @Operation(summary = "调整草稿候选", description = "提交评审前调整候选讲师（只能来自来源推演记录，排序按当时得分固化）")
    public ApiResponse<PlanResponse> updateCandidates(@PathVariable Long id,
                                                      @Valid @RequestBody PlanUpdateCandidatesRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        return ApiResponse.success("候选已更新",
                planService.updateCandidates(id, request.getInstructorIds(),
                        user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交评审", description = "提交后候选锁定，等待讲师管理员与课程管理员两类意见")
    public ApiResponse<PlanResponse> submit(@PathVariable Long id) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        return ApiResponse.success("已提交评审",
                planService.submit(id, user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/reviews/instructor")
    @Operation(summary = "讲师管理员评审", description = "核对入选讲师资质与可授课状态")
    public ApiResponse<PlanResponse> reviewByInstructorAdmin(@PathVariable Long id,
                                                             @Valid @RequestBody PlanReviewRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole(INSTRUCTOR_ADMIN);
        return ApiResponse.success("评审意见已提交",
                planService.reviewByInstructorAdmin(id, request.getDecision(), request.getComment(),
                        user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/reviews/course")
    @Operation(summary = "课程管理员评审", description = "核对课程要求与方案采用的权重配置")
    public ApiResponse<PlanResponse> reviewByCourseAdmin(@PathVariable Long id,
                                                        @Valid @RequestBody PlanReviewRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        return ApiResponse.success("评审意见已提交",
                planService.reviewByCourseAdmin(id, request.getDecision(), request.getComment(),
                        user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "系统管理员驳回", description = "可在评审阶段驳回，但不能代替任何一类专业评审通过")
    public ApiResponse<PlanResponse> reject(@PathVariable Long id,
                                           @Valid @RequestBody PlanRejectRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole(ADMIN);
        return ApiResponse.success("已驳回",
                planService.reject(id, request.getReason(), user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布方案", description = "两类专业评审均通过后发布；同课程同时刻仅一个有效发布版本，并发/重试安全")
    public ApiResponse<PlanResponse> publish(@PathVariable Long id) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        return ApiResponse.success("发布成功",
                planService.publish(id, user.getUsername(), user.getRealName()));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "正式指派", description = "对当前有效发布版本发起正式指派；指派前再次复核讲师当前容量与可用状态")
    public ApiResponse<PlanAssignmentResultResponse> assign(@PathVariable Long id) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        PlanAssignmentResultResponse result =
                planAssignmentService.assignFromPlan(id, user.getUsername(), user.getRealName());
        return ApiResponse.success(result.isSuccess() ? "正式指派成功" : "正式指派失败", result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "废弃草稿", description = "仅未发布方案可废弃；已发布版本只能被新版本替代并保留")
    public ApiResponse<Void> discard(@PathVariable Long id) {
        AuthUser user = CurrentUserProvider.requireAnyRole(COURSE_ADMIN);
        planService.discard(id, user.getUsername(), user.getRealName());
        return ApiResponse.success("已废弃", null);
    }

    // ---------------- 只读（任何已登录角色，含 VIEWER） ----------------

    @GetMapping
    @Operation(summary = "方案列表", description = "可按课程/状态筛选；查看者只读")
    public ApiResponse<List<PlanResponse>> list(@RequestParam(required = false) Long courseId,
                                                @RequestParam(required = false) String status) {
        CurrentUserProvider.require();
        return ApiResponse.success(planService.listPlans(courseId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "方案详情", description = "版本、快照、候选排序、评审进度、失效原因、发布/指派结果与轨迹")
    public ApiResponse<PlanResponse> getById(@PathVariable Long id) {
        CurrentUserProvider.require();
        return ApiResponse.success(planService.getPlan(id));
    }

    @GetMapping("/{id}/diff")
    @Operation(summary = "快照与当前资料差异", description = "查看课程要求/权重/候选讲师资料相对快照的变化，仅展示不重算")
    public ApiResponse<PlanSnapshotDiffResponse> getDiff(@PathVariable Long id) {
        CurrentUserProvider.require();
        return ApiResponse.success(planService.getDiff(id));
    }
}
