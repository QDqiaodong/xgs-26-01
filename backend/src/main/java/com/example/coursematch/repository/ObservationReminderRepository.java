package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObservationReminderRepository extends JpaRepository<ObservationReminder, Long> {
    List<ObservationReminder> findByStatusOrderByCreatedAtDesc(String status);
    List<ObservationReminder> findByObservationIdOrderByCreatedAtDesc(Long observationId);
    Optional<ObservationReminder> findByObservationIdAndReminderTypeAndDedupeKey(Long observationId, String type, String dedupeKey);
}
