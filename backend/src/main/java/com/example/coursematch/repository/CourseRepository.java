package com.example.coursematch.repository;

import com.example.coursematch.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatus(String status);
    List<Course> findByIndustryAndStatus(String industry, String status);
    List<Course> findByIndustry(String industry);
    
    Page<Course> findAll(Pageable pageable);
    Page<Course> findByIndustry(String industry, Pageable pageable);
    Page<Course> findByStatus(String status, Pageable pageable);
    Page<Course> findByIndustryAndStatus(String industry, String status, Pageable pageable);
    
    @Query("SELECT c FROM Course c WHERE (:industry IS NULL OR c.industry = :industry) AND (:status IS NULL OR c.status = :status)")
    Page<Course> findByIndustryAndStatusOptional(@Param("industry") String industry, @Param("status") String status, Pageable pageable);
    
    long countByStatus(String status);
    
    @Query("SELECT c.industry, COUNT(c) FROM Course c GROUP BY c.industry")
    List<Object[]> countByIndustry();
}