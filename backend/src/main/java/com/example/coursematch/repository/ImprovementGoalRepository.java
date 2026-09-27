package com.example.coursematch.repository;

import com.example.coursematch.entity.ImprovementGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImprovementGoalRepository extends JpaRepository<ImprovementGoal, Long> {

    List<ImprovementGoal> findByObservationIdOrderByStageNoAscIdAsc(Long observationId);

    long countByObservationIdAndStatus(Long observationId, String status);
}
