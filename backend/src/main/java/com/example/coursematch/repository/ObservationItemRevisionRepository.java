package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationItemRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationItemRevisionRepository extends JpaRepository<ObservationItemRevision, Long> {

    List<ObservationItemRevision> findByObservationIdOrderByIdAsc(Long observationId);

    List<ObservationItemRevision> findByItemIdOrderByIdAsc(Long itemId);
}
