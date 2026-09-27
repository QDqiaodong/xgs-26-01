package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationReopenRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObservationReopenRequestRepository extends JpaRepository<ObservationReopenRequest, Long> {

    List<ObservationReopenRequest> findByObservationIdOrderByIdAsc(Long observationId);

    /** 同一观察单同时只允许一个待审批的重开申请（重复点击/超时重试防护） */
    Optional<ObservationReopenRequest> findFirstByObservationIdAndStatusOrderByIdDesc(Long observationId, String status);

    List<ObservationReopenRequest> findByStatusOrderByIdAsc(String status);
}
