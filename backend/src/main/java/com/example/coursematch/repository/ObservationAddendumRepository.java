package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationAddendum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationAddendumRepository extends JpaRepository<ObservationAddendum, Long> {
    List<ObservationAddendum> findByObservationIdOrderByCreatedAtAsc(Long observationId);
}
