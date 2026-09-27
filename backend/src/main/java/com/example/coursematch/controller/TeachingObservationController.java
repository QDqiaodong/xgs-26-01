package com.example.coursematch.controller;

import com.example.coursematch.dto.request.TeachingObservationRequests.*;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.ObservationSummaryResponse;
import com.example.coursematch.dto.response.TeachingObservationResponse;
import com.example.coursematch.security.AuthUser;
import com.example.coursematch.security.CurrentUserProvider;
import com.example.coursematch.service.TeachingObservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 讲师带教观察与改进周期。所有权限和脱敏均由服务端执行。
 */
@RestController
@RequestMapping("/api/teaching-observations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "讲师带教观察与改进周期", description = "分级可见、异议隔离复核、阶段目标、证据链、重开版本与并发防护")
public class TeachingObservationController {

    private final TeachingObservationService service;

    @PostMapping
    @Operation(summary = "按课程场次建立观察")
    public ApiResponse<TeachingObservationResponse> create(@Valid @RequestBody CreateObservation request) {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success("观察已建立，原始事实已分级保存", service.createObservation(request, user));
    }

    @PostMapping("/{id}/addenda")
    @Operation(summary = "追加事后说明", description = "只能追加，不修改原始观察项")
    public ApiResponse<TeachingObservationResponse> addendum(@PathVariable Long id,
                                                             @Valid @RequestBody AddendumInput request) {
        return ApiResponse.success("补充说明已追加", service.addAddendum(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/acknowledge")
    public ApiResponse<TeachingObservationResponse> acknowledge(@PathVariable Long id,
                                                                @RequestBody(required = false) AcknowledgeRequest request) {
        AuthUser user = CurrentUserProvider.require();
        return ApiResponse.success("已确认", service.acknowledge(id, request == null ? new AcknowledgeRequest() : request, user));
    }

    @PostMapping("/{id}/respond")
    public ApiResponse<TeachingObservationResponse> respond(@PathVariable Long id,
                                                            @Valid @RequestBody RespondRequest request) {
        return ApiResponse.success("回应已提交", service.respond(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/refuse")
    public ApiResponse<TeachingObservationResponse> refuse(@PathVariable Long id,
                                                           @Valid @RequestBody RefuseRequest request) {
        return ApiResponse.success("已记录拒绝确认", service.refuse(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/disputes")
    public ApiResponse<TeachingObservationResponse> dispute(@PathVariable Long id,
                                                            @Valid @RequestBody DisputeRequest request) {
        return ApiResponse.success("异议已提交，等待另一名督导复核", service.raiseDispute(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/disputes/review")
    public ApiResponse<TeachingObservationResponse> reviewDispute(@PathVariable Long id,
                                                                  @Valid @RequestBody DisputeReviewRequest request) {
        return ApiResponse.success("异议复核完成，原始版本与修正版本均可追溯",
                service.reviewDispute(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/goals")
    public ApiResponse<TeachingObservationResponse> goals(@PathVariable Long id,
                                                          @Valid @RequestBody GoalsRequest request) {
        return ApiResponse.success("分阶段目标已保存，等待双方确认后激活", service.saveGoals(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/goals/confirm")
    public ApiResponse<TeachingObservationResponse> confirmGoals(@PathVariable Long id,
                                                                 @RequestBody(required = false) AcknowledgeRequest request) {
        return ApiResponse.success("阶段目标确认已记录",
                service.confirmGoals(id, request == null ? null : request.getExpectedVersion(), CurrentUserProvider.require()));
    }

    @PostMapping("/goals/{goalId}/evidence")
    public ApiResponse<TeachingObservationResponse> evidence(@PathVariable Long goalId,
                                                             @Valid @RequestBody EvidenceRequest request) {
        return ApiResponse.success("进展与证明已提交", service.submitEvidence(goalId, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/evidence/review")
    public ApiResponse<TeachingObservationResponse> reviewEvidence(@PathVariable Long id,
                                                                   @Valid @RequestBody EvidenceReviewRequest request) {
        return ApiResponse.success("证明复查完成", service.reviewEvidence(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<TeachingObservationResponse> close(@PathVariable Long id,
                                                          @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("周期已结案", service.closeCycle(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/extend")
    public ApiResponse<TeachingObservationResponse> extend(@PathVariable Long id,
                                                           @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("周期已延长", service.extendCycle(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/escalation-request")
    public ApiResponse<TeachingObservationResponse> requestEscalation(@PathVariable Long id,
                                                                      @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("升级申请已提交", service.requestEscalation(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/escalate")
    public ApiResponse<TeachingObservationResponse> escalate(@PathVariable Long id,
                                                             @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success("已升级为强化辅导", service.escalateCycle(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/reopen")
    public ApiResponse<TeachingObservationResponse> reopen(@PathVariable Long id,
                                                           @Valid @RequestBody ReopenRequest request) {
        return ApiResponse.success("重开申请已提交", service.requestReopen(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/reopen/approve")
    public ApiResponse<TeachingObservationResponse> approveReopen(@PathVariable Long id,
                                                                  @Valid @RequestBody ReopenDecisionRequest request) {
        return ApiResponse.success("已批准重开并恢复待复查", service.approveReopen(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/reopen/decline")
    public ApiResponse<TeachingObservationResponse> declineReopen(@PathVariable Long id,
                                                                  @Valid @RequestBody ReopenDecisionRequest request) {
        return ApiResponse.success("已驳回重开申请", service.declineReopen(id, request, CurrentUserProvider.require()));
    }

    @PostMapping("/{id}/reopened-conclusion")
    public ApiResponse<TeachingObservationResponse> reopenedConclusion(@PathVariable Long id,
                                                                       @Valid @RequestBody NewConclusionRequest request) {
        return ApiResponse.success("新结论已登记，旧结论保留为历史版本",
                service.createReopenedConclusion(id, request, CurrentUserProvider.require()));
    }

    @GetMapping
    @Operation(summary = "观察/周期列表", description = "普通查看者仅见状态与最终结论；讲师仅见本人；督导/负责人见授权范围完整信息")
    public ApiResponse<List<ObservationSummaryResponse>> list(@RequestParam(required = false) Long courseId,
                                                              @RequestParam(required = false) Long instructorId,
                                                              @RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(CurrentUserProvider.require(), courseId, instructorId, status));
    }

    @GetMapping("/reminders")
    public ApiResponse<List<TeachingObservationResponse.ReminderDto>> reminders() {
        return ApiResponse.success(service.myPendingReminders(CurrentUserProvider.require()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "观察详情", description = "返回当前有效观察项、目标进度、待处理人、周期结论、历史版本和完整时间线")
    public ApiResponse<TeachingObservationResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getObservation(id, CurrentUserProvider.require()));
    }
}
