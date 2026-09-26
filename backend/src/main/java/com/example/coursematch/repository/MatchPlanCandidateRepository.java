package com.example.coursematch.repository;

import com.example.coursematch.entity.MatchPlanCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchPlanCandidateRepository extends JpaRepository<MatchPlanCandidate, Long> {

    List<MatchPlanCandidate> findByPlanIdOrderByRankNoAsc(Long planId);

    List<MatchPlanCandidate> findByInstructorId(Long instructorId);

    long countByPlanId(Long planId);
}
