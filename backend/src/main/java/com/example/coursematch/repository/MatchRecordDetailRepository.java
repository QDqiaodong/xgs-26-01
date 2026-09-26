package com.example.coursematch.repository;

import com.example.coursematch.entity.MatchRecordDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRecordDetailRepository extends JpaRepository<MatchRecordDetail, Long> {
    List<MatchRecordDetail> findByRecordIdOrderByRankAsc(Long recordId);
    void deleteByRecordId(Long recordId);
}