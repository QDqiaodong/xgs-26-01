package com.example.coursematch.repository;

import com.example.coursematch.entity.GoalEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalEvidenceRepository extends JpaRepository<GoalEvidence, Long> {

    List<GoalEvidence> findByGoalIdOrderByIdAsc(Long goalId);

    List<GoalEvidence> findByObservationIdOrderByIdAsc(Long observationId);

    long countByGoalIdAndStatus(Long goalId, String status);
}
