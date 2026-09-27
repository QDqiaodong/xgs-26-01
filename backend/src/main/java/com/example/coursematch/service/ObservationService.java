package com.example.coursematch.service;

import com.example.coursematch.dto.request.*;
import com.example.coursematch.dto.response.ObservationListItemResponse;
import com.example.coursematch.dto.response.ObservationReminderResponse;
import com.example.coursematch.dto.response.ObservationResponse;
import com.example.coursematch.security.AuthUser;

import java.util.List;

/**
 * 讲师带教观察与改进周期。
 *
 * <p>所有写操作在服务端强制角色与归属：督导建观察/追加/复查异议/复查证据，
 * 讲师确认/回应/提异议/回报进展，课程负责人结案/延长/升级/审批重开，查看者只读公开层。
 */
public interface ObservationService {

    // ---------------- 观察 ----------------
    ObservationResponse create(ObservationCreateRequest request, AuthUser user);

    ObservationResponse addAddendum(Long observationId, ObservationAddendumRequest request, AuthUser user);

    ObservationResponse getDetail(Long observationId, AuthUser user);

    List<ObservationListItemResponse> list(Long courseId, Long instructorId, String status, AuthUser user);

    // ---------------- 确认/回应/异议 ----------------
    ObservationResponse acknowledge(Long observationId, ObservationAckRequest request, AuthUser user);

    ObservationResponse raiseDispute(Long observationId, DisputeRaiseRequest request, AuthUser user);

    ObservationResponse reviewDispute(Long observationId, Long disputeId, DisputeReviewRequest request, AuthUser user);

    // ---------------- 目标与证据 ----------------
    ObservationResponse createGoal(Long observationId, GoalCreateRequest request, AuthUser user);

    ObservationResponse submitEvidence(Long observationId, Long goalId, EvidenceSubmitRequest request, AuthUser user);

    ObservationResponse reviewEvidence(Long observationId, Long evidenceId, EvidenceReviewRequest request, AuthUser user);

    ObservationResponse supplementEvidence(Long observationId, Long evidenceId,
                                           EvidenceSupplementRequest request, AuthUser user);

    // ---------------- 周期决策与重开 ----------------
    ObservationResponse decideCycle(Long observationId, CycleDecisionRequest request, AuthUser user);

    ObservationResponse requestReopen(Long observationId, ReopenRequest request, AuthUser user);

    ObservationResponse decideReopen(Long observationId, Long reopenId,
                                     ReopenDecisionRequest request, AuthUser user);

    ObservationResponse submitRereview(Long observationId, RereviewRequest request, AuthUser user);

    // ---------------- 提醒与逾期 ----------------
    List<ObservationReminderResponse> myReminders(AuthUser user);

    /** 逾期扫描：确认/回应逾期、督导长期未复查。定时任务与手动触发共用，去重不重复提醒。 */
    int scanOverdue();
}
