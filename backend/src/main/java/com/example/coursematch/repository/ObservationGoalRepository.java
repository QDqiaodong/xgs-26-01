package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationGoal;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;

public interface ObservationGoalRepository extends JpaRepository<ObservationGoal, Long> {
    List<ObservationGoal> findByObservationIdOrderByStageNoAscIdAsc(Long observationId);
    long countByObservationIdAndRequiredTrueAndStatusNot(Long observationId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select g from ObservationGoal g where g.id = :id")
    Optional<ObservationGoal> findByIdForUpdate(Long id);
}
