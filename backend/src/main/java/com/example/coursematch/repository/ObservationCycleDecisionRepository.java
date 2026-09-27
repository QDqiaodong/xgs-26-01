package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationCycleDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObservationCycleDecisionRepository extends JpaRepository<ObservationCycleDecision, Long> {
    List<ObservationCycleDecision> findByObservationIdOrderByVersionNoAscIdAsc(Long observationId);
    long countByObservationId(Long observationId);
    Optional<ObservationCycleDecision> findByObservationIdAndClientToken(Long observationId, String clientToken);
}
