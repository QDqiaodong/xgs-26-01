package com.example.coursematch.repository;

import com.example.coursematch.entity.PlanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanEventRepository extends JpaRepository<PlanEvent, Long> {

    List<PlanEvent> findByPlanIdOrderByIdAsc(Long planId);
}
