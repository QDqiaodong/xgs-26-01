package com.example.coursematch.controller;

import com.example.coursematch.dto.request.*;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.ObservationListItemResponse;
import com.example.coursematch.dto.response.ObservationReminderResponse;
import com.example.coursematch.dto.response.ObservationResponse;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.CurrentUserProvider;
import com.example.coursematch.service.ObservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 讲师带教观察与改进周期。
 *
 * <p>角色边界（服务端强制，前端隐藏按钮仅为体验）：
 * <ul>
 *   <li>教学督导 SUPERVISOR：建立观察、追加说明、（另一名督导）复核异议、复查证据、重开复查</li>
 *   <li>讲师 INSTRUCTOR：确认已阅/回应/拒绝、确认前提异议、共同形成目标、回报与补充证据、申请重开</li>
 *   <li>课程负责人 COURSE_ADMIN：结案（必需目标均有有效证据后）、写明依据的延长/升级、审批重开</li>
 *   <li>查看者 VIEWER：只看到周期状态、进度与最终结论，看不到观察明细与敏感内容</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/observations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "讲师带教观察与改进周期",
        description = "分级观察、确认回应、异议隔离复核、分阶段目标与证据、结案/延长/升级/重开版本链")
public class ObservationController {

    private final ObservationService observationService;

    // ---------------- 观察 ----------------

    @PostMapping
    @Operation(summary = "建立带教观察", description = "督导按课程场次建立，四维度事实/等级/建议分级保存，生成讲师确认待办")
    public ApiResponse<ObservationResponse> create(@Valid @RequestBody ObservationCreateRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("SUPERVISOR");
        return ApiResponse.success("观察已建立，已生成讲师确认待办", observationService.create(request, user));
    }

    @PostMapping("/{id}/addenda")
    @Operation(summary = "追加补充说明", description = "事后补充只能追加，不修改原始观察；按分级保存")
    public ApiResponse<ObservationResponse> addAddendum(@PathVariable Long id,
                                                        @Valid @RequestBody ObservationAddendumRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("SUPERVISOR");
        return ApiResponse.success("补充说明已追加，原始记录未改动",
                observationService.addAddendum(id, request, user));
    }

    @GetMapping
    @Operation(summary = "观察列表", description = "按角色分级返回；讲师仅本人，查看者仅状态/进度/结论")
    public ApiResponse<List<ObservationListItemResponse>> list(@RequestParam(required = false) Long courseId,
                                                               @RequestParam(required = false) Long instructorId,
                                                               @RequestParam(required = false) String status) {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success(observationService.list(courseId, instructorId, status, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "观察详情", description = "分级可见：完整/脱敏改进相关/仅状态结论三个视图")
    public ApiResponse<ObservationResponse> getById(@PathVariable Long id) {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success(observationService.getDetail(id, user));
    }

    // ---------------- 确认/回应/异议 ----------------

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "讲师确认已阅并回应", description = "确认后按当前有效项判定是否进入改进；拒绝确认须填理由，进入独立待处理状态")
    public ApiResponse<ObservationResponse> acknowledge(@PathVariable Long id,
                                                        @Valid @RequestBody ObservationAckRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("INSTRUCTOR");
        return ApiResponse.success("已记录你的确认与回应", observationService.acknowledge(id, request, user));
    }

    @PostMapping("/{id}/disputes")
    @Operation(summary = "讲师提出事实异议", description = "只能在确认前提出；同一轮重复提出得到409")
    public ApiResponse<ObservationResponse> raiseDispute(@PathVariable Long id,
                                                         @Valid @RequestBody DisputeRaiseRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("INSTRUCTOR");
        return ApiResponse.success("异议已提交，将由另一名教学督导复核",
                observationService.raiseDispute(id, request, user));
    }

    @PostMapping("/{id}/disputes/{disputeId}/review")
    @Operation(summary = "另一名督导复核异议",
            description = "可维持/修正/撤销具体观察项；原观察人禁止复核；原文字原等级留痕；重复处理返回409")
    public ApiResponse<ObservationResponse> reviewDispute(@PathVariable Long id,
                                                          @PathVariable Long disputeId,
                                                          @Valid @RequestBody DisputeReviewRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("SUPERVISOR");
        return ApiResponse.success("异议复核完成，观察单回到待确认",
                observationService.reviewDispute(id, disputeId, request, user));
    }

    // ---------------- 改进目标与证据 ----------------

    @PostMapping("/{id}/goals")
    @Operation(summary = "形成分阶段改进目标", description = "督导与讲师共同形成；每项必须有衡量标准、截止时间和证明材料要求")
    public ApiResponse<ObservationResponse> createGoal(@PathVariable Long id,
                                                       @Valid @RequestBody GoalCreateRequest request) {
        AuthUser user = CurrentUserProvider.require();
        if (!user.hasRole("SUPERVISOR") && !user.hasRole("INSTRUCTOR")) {
            throw new com.example.coursematch.security.ForbiddenException("仅督导或讲师可以维护改进目标");
        }
        return ApiResponse.success("改进目标已形成", observationService.createGoal(id, request, user));
    }

    @PostMapping("/{id}/goals/{goalId}/evidences")
    @Operation(summary = "讲师回报证据", description = "重复提交不覆盖已有证据，每次回报独立待复查")
    public ApiResponse<ObservationResponse> submitEvidence(@PathVariable Long id,
                                                           @PathVariable Long goalId,
                                                           @Valid @RequestBody EvidenceSubmitRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("INSTRUCTOR");
        return ApiResponse.success("证据已提交，等待督导复查",
                observationService.submitEvidence(id, goalId, request, user));
    }

    @PostMapping("/{id}/evidences/{evidenceId}/review")
    @Operation(summary = "督导复查证据", description = "通过/退回不充分证明/要求补充；重复复查返回409")
    public ApiResponse<ObservationResponse> reviewEvidence(@PathVariable Long id,
                                                           @PathVariable Long evidenceId,
                                                           @Valid @RequestBody EvidenceReviewRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("SUPERVISOR");
        return ApiResponse.success("复查意见已提交", observationService.reviewEvidence(id, evidenceId, request, user));
    }

    @PostMapping("/{id}/evidences/{evidenceId}/supplement")
    @Operation(summary = "讲师追加补充材料", description = "针对“要求补充”追加说明，不改变原回报内容")
    public ApiResponse<ObservationResponse> supplementEvidence(@PathVariable Long id,
                                                               @PathVariable Long evidenceId,
                                                               @Valid @RequestBody EvidenceSupplementRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("INSTRUCTOR");
        return ApiResponse.success("补充材料已提交，重新等待复查",
                observationService.supplementEvidence(id, evidenceId, request, user));
    }

    // ---------------- 周期决策与重开 ----------------

    @PostMapping("/{id}/decision")
    @Operation(summary = "负责人周期决策",
            description = "结案仅在必需目标均有有效证据后允许；延长/升级必须写明依据；重复结案返回409")
    public ApiResponse<ObservationResponse> decide(@PathVariable Long id,
                                                   @Valid @RequestBody CycleDecisionRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("COURSE_ADMIN");
        return ApiResponse.success("周期决策已记录", observationService.decideCycle(id, request, user));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "申请重开", description = "结案后发现证据错误时由督导/讲师/负责人发起，等待负责人批准")
    public ApiResponse<ObservationResponse> requestReopen(@PathVariable Long id,
                                                          @Valid @RequestBody ReopenRequest request) {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success("重开申请已提交，等待课程负责人审批",
                observationService.requestReopen(id, request, user));
    }

    @PostMapping("/{id}/reopen/{reopenId}/decision")
    @Operation(summary = "负责人审批重开", description = "批准后恢复到待复查，原结论保留为历史版本；重复审批返回409")
    public ApiResponse<ObservationResponse> decideReopen(@PathVariable Long id,
                                                         @PathVariable Long reopenId,
                                                         @Valid @RequestBody ReopenDecisionRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("COURSE_ADMIN");
        return ApiResponse.success("重开审批已完成", observationService.decideReopen(id, reopenId, request, user));
    }

    @PostMapping("/{id}/rereview")
    @Operation(summary = "督导提交重开复查意见", description = "重开批准后由原观察督导复查，随后负责人再次结案形成新版结论")
    public ApiResponse<ObservationResponse> rereview(@PathVariable Long id,
                                                     @Valid @RequestBody RereviewRequest request) {
        AuthUser user = CurrentUserProvider.requireAnyRole("SUPERVISOR");
        return ApiResponse.success("复查意见已提交", observationService.submitRereview(id, request, user));
    }

    // ---------------- 提醒 ----------------

    @GetMapping("/reminders/mine")
    @Operation(summary = "我的待处理提醒",
            description = "确认逾期/拒绝/异议复核/证据复查/复查逾期/负责人结案/重开审批等独立提醒")
    public ApiResponse<List<ObservationReminderResponse>> myReminders() {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success(observationService.myReminders(user));
    }

    @PostMapping("/overdue/scan")
    @Operation(summary = "触发逾期扫描", description = "定时任务与手动触发共用；状态推进与提醒去重，不重复")
    public ApiResponse<Integer> scanOverdue() {
        CurrentUserProvider.requireAnyRole("SUPERVISOR", "COURSE_ADMIN", "ADMIN");
        return ApiResponse.success("逾期扫描完成", observationService.scanOverdue());
    }
}
