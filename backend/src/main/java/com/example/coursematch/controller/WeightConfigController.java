package com.example.coursematch.controller;

import com.example.coursematch.dto.request.WeightConfigRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.entity.WeightConfig;
import com.example.coursematch.service.WeightConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weights")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WeightConfigController {

    private final WeightConfigService weightConfigService;

    @GetMapping
    public ApiResponse<List<WeightConfig>> getAllWeightConfigs() {
        return ApiResponse.success(weightConfigService.getAllWeightConfigs());
    }

    @GetMapping("/{id}")
    public ApiResponse<WeightConfig> getWeightConfigById(@PathVariable Long id) {
        return weightConfigService.getWeightConfigById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "权重配置不存在"));
    }

    @GetMapping("/default")
    public ApiResponse<WeightConfig> getDefaultConfig() {
        return ApiResponse.success(weightConfigService.getDefaultConfig());
    }

    @PostMapping
    public ApiResponse<WeightConfig> createWeightConfig(@RequestBody WeightConfigRequest request) {
        return ApiResponse.success("创建成功", weightConfigService.createWeightConfig(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WeightConfig> updateWeightConfig(@PathVariable Long id, @RequestBody WeightConfigRequest request) {
        return ApiResponse.success("更新成功", weightConfigService.updateWeightConfig(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWeightConfig(@PathVariable Long id) {
        weightConfigService.deleteWeightConfig(id);
        return ApiResponse.success("删除成功", null);
    }
}