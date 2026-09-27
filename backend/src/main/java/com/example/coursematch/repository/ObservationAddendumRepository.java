package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationAddendum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationAddendumRepository extends JpaRepository<ObservationAddendum, Long> {

    List<ObservationAddendum> findByObservationIdOrderByIdAsc(Long observationId);
}
