package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationEventRepository extends JpaRepository<ObservationEvent, Long> {

    List<ObservationEvent> findByObservationIdOrderByIdAsc(Long observationId);
}
