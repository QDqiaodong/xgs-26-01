package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationItemRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationItemRevisionRepository extends JpaRepository<ObservationItemRevision, Long> {
    List<ObservationItemRevision> findByObservationIdOrderByRevisionNoAscIdAsc(Long observationId);
    List<ObservationItemRevision> findByItemIdOrderByRevisionNoAsc(Long itemId);
}
