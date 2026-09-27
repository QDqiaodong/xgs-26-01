package com.example.coursematch.repository;

import com.example.coursematch.entity.ObservationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationAttachmentRepository extends JpaRepository<ObservationAttachment, Long> {

    List<ObservationAttachment> findByObservationIdOrderByIdAsc(Long observationId);
}
