package com.example.coursematch.repository;

import com.example.coursematch.entity.CycleConclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CycleConclusionRepository extends JpaRepository<CycleConclusion, Long> {

    List<CycleConclusion> findByObservationIdOrderByVersionNoAsc(Long observationId);

    long countByObservationId(Long observationId);
}
