package com.example.coursematch.repository;

import com.example.coursematch.entity.GapWarningRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GapWarningRuleRepository extends JpaRepository<GapWarningRule, Long> {
    List<GapWarningRule> findByIsActiveTrue();
    List<GapWarningRule> findByTargetIndustryAndIsActiveTrue(String targetIndustry);
    List<GapWarningRule> findByWarningLevelAndIsActiveTrue(String warningLevel);
}