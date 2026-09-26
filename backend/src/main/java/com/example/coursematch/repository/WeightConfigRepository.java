package com.example.coursematch.repository;

import com.example.coursematch.entity.WeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeightConfigRepository extends JpaRepository<WeightConfig, Long> {
    List<WeightConfig> findByActiveTrue();
    Optional<WeightConfig> findByIdAndActiveTrue(Long id);
    List<WeightConfig> findByIsDefaultTrue();
}