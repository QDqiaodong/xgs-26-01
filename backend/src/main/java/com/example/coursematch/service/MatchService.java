package com.example.coursematch.service;

import com.example.coursematch.dto.request.BatchMatchRequest;
import com.example.coursematch.dto.request.MatchRequest;
import com.example.coursematch.dto.response.GapWarningResponse;
import com.example.coursematch.dto.response.MatchResultResponse;
import com.example.coursematch.entity.MatchRecord;
import com.example.coursematch.entity.MatchRecordDetail;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MatchService {
    MatchResultResponse matchInstructors(MatchRequest request);
    List<MatchResultResponse> batchMatch(BatchMatchRequest request);
    List<MatchRecord> getAllMatchRecords(int page, int size);
    MatchRecord getMatchRecordById(Long id);
    List<MatchRecordDetail> getMatchRecordDetails(Long recordId);
    List<GapWarningResponse> getGapWarnings();
    void deleteMatchRecord(Long id);
    List<MatchRecord> getMatchRecordsByCourseId(Long courseId);
    List<MatchRecord> getMatchRecordsByIndustry(String industry);
}