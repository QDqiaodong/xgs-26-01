package com.example.coursematch.repository;

import com.example.coursematch.entity.TeachingObservation;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingObservationRepository extends JpaRepository<TeachingObservation, Long> {

    List<TeachingObservation> findByInstructorIdOrderByObservedAtDesc(Long instructorId);

    List<TeachingObservation> findByCourseIdOrderByObservedAtDesc(Long courseId);

    List<TeachingObservation> findAllByOrderByObservedAtDesc();

    List<TeachingObservation> findByStatusInOrderByObservedAtDesc(List<String> statuses);

    /** 悲观写锁锁定观察单，串行化确认/异议/复查/结案/重开等并发状态推进。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select o from TeachingObservation o where o.id = :id")
    Optional<TeachingObservation> findByIdForUpdate(Long id);

    /** 逾期扫描用：处于待确认状态且带确认截止时间的观察 */
    @Query("select o from TeachingObservation o where o.status = 'PENDING_ACK' and o.ackDueAt is not null and o.ackDueAt < :now")
    List<TeachingObservation> findAckOverdue(java.time.LocalDateTime now);

    /** 复查逾期扫描：证据已提交等待督导复查 */
    @Query("select o from TeachingObservation o where o.status = 'EVIDENCE_PENDING_REVIEW'")
    List<TeachingObservation> findEvidencePendingReview();
}
