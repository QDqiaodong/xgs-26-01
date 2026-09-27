package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObservationDisputeRepository extends JpaRepository<ObservationDispute, Long> {

    List<ObservationDispute> findByObservationIdOrderByIdAsc(Long observationId);

    /** 当前观察单是否已有指定轮次的异议（重复提交防护） */
    Optional<ObservationDispute> findByObservationIdAndRoundNo(Long observationId, Integer roundNo);

    List<ObservationDispute> findByStatusOrderByRaisedAtAsc(String status);
}
