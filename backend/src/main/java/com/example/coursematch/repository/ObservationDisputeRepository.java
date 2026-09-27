package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationDispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ObservationDisputeRepository extends JpaRepository<ObservationDispute, Long> {
    List<ObservationDispute> findByObservationIdOrderByRaisedAtDesc(Long observationId);
    Optional<ObservationDispute> findFirstByObservationIdAndStatusOrderByRaisedAtDesc(Long observationId, String status);
    boolean existsByObservationIdAndStatus(Long observationId, String status);
}
