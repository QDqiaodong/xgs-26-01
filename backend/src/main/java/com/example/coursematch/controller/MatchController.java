package com.example.coursematch.controller;

import com.example.coursematch.dto.request.BatchMatchRequest;
import com.example.coursematch.dto.request.MatchRequest;
import com.example.coursematch.dto.response.ApiResponse;
import com.example.coursematch.dto.response.GapWarningResponse;
import com.example.coursematch.dto.response.MatchResultResponse;
import com.example.coursematch.entity.MatchRecord;
import com.example.coursematch.entity.MatchRecordDetail;
import com.example.coursematch.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ApiResponse<MatchResultResponse> matchInstructors(@RequestBody MatchRequest request) {
        return ApiResponse.success(matchService.matchInstructors(request));
    }

    @PostMapping("/batch")
    public ApiResponse<List<MatchResultResponse>> batchMatch(@RequestBody BatchMatchRequest request) {
        return ApiResponse.success(matchService.batchMatch(request));
    }

    @GetMapping
    public ApiResponse<List<MatchRecord>> getAllMatchRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(matchService.getAllMatchRecords(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MatchRecord> getMatchRecordById(@PathVariable Long id) {
        return ApiResponse.success(matchService.getMatchRecordById(id));
    }

    @GetMapping("/{id}/details")
    public ApiResponse<List<MatchRecordDetail>> getMatchRecordDetails(@PathVariable Long id) {
        return ApiResponse.success(matchService.getMatchRecordDetails(id));
    }

    @GetMapping("/gaps")
    public ApiResponse<List<GapWarningResponse>> getGapWarnings() {
        return ApiResponse.success(matchService.getGapWarnings());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMatchRecord(@PathVariable Long id) {
        matchService.deleteMatchRecord(id);
        return ApiResponse.success("删除成功", null);
    }
}