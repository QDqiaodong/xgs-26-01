package com.example.coursematch.repository;

import com.example.coursematch.entity.TeachingObservation;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TeachingObservationRepository extends JpaRepository<TeachingObservation, Long> {
    List<TeachingObservation> findByCourseIdOrderByOccurredAtDesc(Long courseId);
    List<TeachingObservation> findByInstructorIdOrderByOccurredAtDesc(Long instructorId);
    List<TeachingObservation> findAllByOrderByOccurredAtDesc();
    List<TeachingObservation> findByStatusInOrderByOccurredAtDesc(List<String> statuses);
    List<TeachingObservation> findByStatusAndResponseDueAtBefore(String status, LocalDateTime time);
    List<TeachingObservation> findByStatusAndFollowupDueAtBefore(String status, LocalDateTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select o from TeachingObservation o where o.id = :id")
    Optional<TeachingObservation> findByIdForUpdate(Long id);
}
