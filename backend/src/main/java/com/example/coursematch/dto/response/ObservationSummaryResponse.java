package com.example.coursematch.dto.response;

import java.time.LocalDateTime;

public record ObservationSummaryResponse(
        Long id,
        Long courseId,
        String courseName,
        String sessionNo,
        Long instructorId,
        String instructorName,
        String observerName,
        LocalDateTime occurredAt,
        String status,
        String statusText,
        boolean needsImprovement,
        String currentConclusion,
        Long pendingReminderCount,
        Long versionLock,
        String viewLevel
) {
}
