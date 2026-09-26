package com.example.coursematch.service;

import com.example.coursematch.dto.request.WeightConfigRequest;
import com.example.coursematch.entity.WeightConfig;

import java.util.List;
import java.util.Optional;

public interface WeightConfigService {
    WeightConfig createWeightConfig(WeightConfigRequest request);
    WeightConfig updateWeightConfig(Long id, WeightConfigRequest request);
    void deleteWeightConfig(Long id);
    Optional<WeightConfig> getWeightConfigById(Long id);
    List<WeightConfig> getAllWeightConfigs();
    WeightConfig getDefaultConfig();
}