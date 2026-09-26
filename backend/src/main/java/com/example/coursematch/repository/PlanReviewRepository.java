package com.example.coursematch.repository;

import com.example.coursematch.entity.PlanReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanReviewRepository extends JpaRepository<PlanReview, Long> {

    List<PlanReview> findByPlanIdOrderByCreatedAtAsc(Long planId);

    Optional<PlanReview> findByPlanIdAndReviewType(Long planId, String reviewType);

    void deleteByPlanId(Long planId);
}
