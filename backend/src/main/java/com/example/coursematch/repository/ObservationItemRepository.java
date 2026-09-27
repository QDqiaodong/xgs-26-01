package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObservationItemRepository extends JpaRepository<ObservationItem, Long> {
    List<ObservationItem> findByObservationIdOrderByIdAsc(Long observationId);
}
