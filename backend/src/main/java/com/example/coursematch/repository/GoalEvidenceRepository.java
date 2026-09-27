package com.example.coursematch.repository;

import com.example.coursematch.entity.GoalEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalEvidenceRepository extends JpaRepository<GoalEvidence, Long> {
    List<GoalEvidence> findByGoalIdOrderBySubmittedAtDescIdDesc(Long goalId);
    List<GoalEvidence> findByObservationIdOrderBySubmittedAtDescIdDesc(Long observationId);
    Optional<GoalEvidence> findByGoalIdAndClientToken(Long goalId, String clientToken);
    boolean existsByGoalIdAndStatus(Long goalId, String status);
}
