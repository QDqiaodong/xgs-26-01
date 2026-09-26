package com.example.coursematch.repository;

import com.example.coursematch.entity.Instructor;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    List<Instructor> findByStatus(String status);

    /** 正式指派时锁定入选讲师行，串行化对同一讲师的并发容量复核 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select i from Instructor i where i.id = :id")
    Optional<Instructor> findByIdForUpdate(@Param("id") Long id);
    
    Page<Instructor> findAll(Pageable pageable);
    Page<Instructor> findByStatus(String status, Pageable pageable);
    Page<Instructor> findByAvailable(Boolean available, Pageable pageable);
    Page<Instructor> findByStatusAndAvailable(String status, Boolean available, Pageable pageable);
    
    @Query("SELECT i FROM Instructor i WHERE (:status IS NULL OR i.status = :status) AND (:available IS NULL OR i.available = :available)")
    Page<Instructor> findByStatusAndAvailableOptional(@Param("status") String status, @Param("available") Boolean available, Pageable pageable);
    
    long countByStatus(String status);
    long countByAvailable(Boolean available);
    
    @Query("SELECT AVG(i.teachingYears) FROM Instructor i WHERE i.status = 'ACTIVE'")
    Double avgTeachingYearsByActive();
    
    @Query("SELECT AVG(i.studentRating) FROM Instructor i WHERE i.status = 'ACTIVE'")
    Double avgStudentRatingByActive();
}