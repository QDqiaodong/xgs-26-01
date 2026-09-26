package com.example.coursematch.service.impl;

import com.example.coursematch.dto.request.WeightConfigRequest;
import com.example.coursematch.entity.WeightConfig;
import com.example.coursematch.repository.WeightConfigRepository;
import com.example.coursematch.service.PlanInvalidationService;
import com.example.coursematch.service.WeightConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeightConfigServiceImpl implements WeightConfigService {

    private final WeightConfigRepository weightConfigRepository;
    private final PlanInvalidationService planInvalidationService;

    /** 权重字段中文名，用于失效轨迹 */
    private static final java.util.Map<String, String> FIELD_LABELS = java.util.Map.of(
            "teachingYearsWeight", "授课年限权重",
            "qualificationWeight", "专业资质权重",
            "ratingWeight", "学员口碑权重",
            "expertiseWeight", "行业擅长权重",
            "styleWeight", "授课风格权重",
            "gapThreshold", "缺口阈值"
    );

    @PostConstruct
    public void initDefaultConfig() {
        if (weightConfigRepository.count() == 0) {
            WeightConfig defaultConfig = new WeightConfig();
            defaultConfig.setName("默认权重配置");
            defaultConfig.setDescription("系统默认的五维度权重配置");
            defaultConfig.setTeachingYearsWeight(0.2);
            defaultConfig.setQualificationWeight(0.2);
            defaultConfig.setRatingWeight(0.25);
            defaultConfig.setExpertiseWeight(0.2);
            defaultConfig.setStyleWeight(0.15);
            defaultConfig.setGapThreshold(60.0);
            weightConfigRepository.save(defaultConfig);
        }
    }

    @Override
    public WeightConfig createWeightConfig(WeightConfigRequest request) {
        WeightConfig config = new WeightConfig();
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setTeachingYearsWeight(request.getTeachingYearsWeight());
        config.setQualificationWeight(request.getQualificationWeight());
        config.setRatingWeight(request.getRatingWeight());
        config.setExpertiseWeight(request.getExpertiseWeight());
        config.setStyleWeight(request.getStyleWeight());
        config.setGapThreshold(request.getGapThreshold());
        return weightConfigRepository.save(config);
    }

    @Override
    @Transactional
    public WeightConfig updateWeightConfig(Long id, WeightConfigRequest request) {
        WeightConfig config = weightConfigRepository.findById(id).orElseThrow();
        List<String> changedFields = new ArrayList<>();
        if (request.getName() != null) config.setName(request.getName());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getTeachingYearsWeight() != null
                && !request.getTeachingYearsWeight().equals(config.getTeachingYearsWeight())) {
            config.setTeachingYearsWeight(request.getTeachingYearsWeight());
            changedFields.add(FIELD_LABELS.get("teachingYearsWeight"));
        }
        if (request.getQualificationWeight() != null
                && !request.getQualificationWeight().equals(config.getQualificationWeight())) {
            config.setQualificationWeight(request.getQualificationWeight());
            changedFields.add(FIELD_LABELS.get("qualificationWeight"));
        }
        if (request.getRatingWeight() != null
                && !request.getRatingWeight().equals(config.getRatingWeight())) {
            config.setRatingWeight(request.getRatingWeight());
            changedFields.add(FIELD_LABELS.get("ratingWeight"));
        }
        if (request.getExpertiseWeight() != null
                && !request.getExpertiseWeight().equals(config.getExpertiseWeight())) {
            config.setExpertiseWeight(request.getExpertiseWeight());
            changedFields.add(FIELD_LABELS.get("expertiseWeight"));
        }
        if (request.getStyleWeight() != null
                && !request.getStyleWeight().equals(config.getStyleWeight())) {
            config.setStyleWeight(request.getStyleWeight());
            changedFields.add(FIELD_LABELS.get("styleWeight"));
        }
        if (request.getGapThreshold() != null
                && !request.getGapThreshold().equals(config.getGapThreshold())) {
            config.setGapThreshold(request.getGapThreshold());
            changedFields.add(FIELD_LABELS.get("gapThreshold"));
        }
        WeightConfig saved = weightConfigRepository.save(config);
        if (!changedFields.isEmpty()) {
            planInvalidationService.onWeightConfigChanged(id,
                    "权重配置「" + saved.getName() + "」被修改", changedFields);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteWeightConfig(Long id) {
        WeightConfig config = weightConfigRepository.findById(id).orElse(null);
        // 删除被方案引用的权重配置，等同于方案所依赖的权重发生变化，未发布方案失效
        planInvalidationService.onWeightConfigChanged(id,
                config != null ? "权重配置「" + config.getName() + "」被删除" : "权重配置被删除",
                List.of("权重配置删除"));
        weightConfigRepository.deleteById(id);
    }

    @Override
    public Optional<WeightConfig> getWeightConfigById(Long id) {
        return weightConfigRepository.findById(id);
    }

    @Override
    public List<WeightConfig> getAllWeightConfigs() {
        return weightConfigRepository.findByActiveTrue();
    }

    @Override
    public WeightConfig getDefaultConfig() {
        return weightConfigRepository.findByActiveTrue().stream()
                .findFirst()
                .orElseThrow();
    }
}