package com.example.coursematch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchMatchRequest {
    private String industry;
    private Long weightConfigId;
}