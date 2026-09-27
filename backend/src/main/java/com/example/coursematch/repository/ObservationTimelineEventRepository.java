package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationTimelineEventRepository extends JpaRepository<ObservationTimelineEvent, Long> {
    List<ObservationTimelineEvent> findByObservationIdOrderByCreatedAtAscIdAsc(Long observationId);
    boolean existsByObservationIdAndEventKey(Long observationId, String eventKey);
}
