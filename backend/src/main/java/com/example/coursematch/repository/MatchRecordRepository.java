package com.example.coursematch.repository;

import com.example.coursematch.entity.MatchRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    Page<MatchRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<MatchRecord> findByCourseId(Long courseId);
    List<MatchRecord> findByCourseIndustry(String industry);
    List<MatchRecord> findByHasGapTrue();
    List<MatchRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Page<MatchRecord> findByCourseNameContainingOrCourseIndustryContaining(String name, String industry, Pageable pageable);
    List<MatchRecord> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    List<MatchRecord> findByCourseIndustryOrderByCreatedAtDesc(String industry);
    List<MatchRecord> findByBatchId(String batchId);
}