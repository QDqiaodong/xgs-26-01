package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObservationReminderRepository extends JpaRepository<ObservationReminder, Long> {

    /** 唯一去重点：同一观察单、同一提醒类型、同一去重键 */
    Optional<ObservationReminder>
    findByObservationIdAndReminderTypeAndDedupKey(Long observationId, String reminderType, String dedupKey);

    List<ObservationReminder> findByAssigneeUsernameAndStatusOrderByCreatedAtDesc(String assigneeUsername, String status);

    List<ObservationReminder> findByStatusOrderByCreatedAtDesc(String status);

    List<ObservationReminder> findByObservationIdAndStatusOrderByIdAsc(Long observationId, String status);

    List<ObservationReminder> findByObservationIdOrderByIdAsc(Long observationId);
}
