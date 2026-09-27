package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationItemRepository extends JpaRepository<ObservationItem, Long> {

    List<ObservationItem> findByObservationIdOrderByIdAsc(Long observationId);
}
