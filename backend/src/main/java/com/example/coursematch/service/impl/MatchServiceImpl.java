package com.example.coursematch.service.impl;

import com.example.coursematch.dto.request.BatchMatchRequest;
import com.example.coursematch.dto.request.MatchRequest;
import com.example.coursematch.dto.response.GapWarningResponse;
import com.example.coursematch.dto.response.MatchResultResponse;
import com.example.coursematch.entity.*;
import com.example.coursematch.repository.*;
import com.example.coursematch.service.MatchService;
import com.example.coursematch.service.OperationLogService;
import com.example.coursematch.service.SkillService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final WeightConfigRepository weightConfigRepository;
    private final MatchRecordRepository matchRecordRepository;
    private final MatchRecordDetailRepository matchRecordDetailRepository;
    private final CourseInstructorAssignmentRepository assignmentRepository;
    private final SkillService skillService;
    private final OperationLogService operationLogService;

    @Override
    @Transactional
    public MatchResultResponse matchInstructors(MatchRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("课程不存在: " + request.getCourseId()));
        
        WeightConfig config = weightConfigRepository.findById(request.getWeightConfigId())
                .orElseGet(this::getDefaultConfig);
        
        List<Instructor> instructors = instructorRepository.findByStatus("ACTIVE");
        List<MatchResultResponse.InstructorMatchDetail> details = new ArrayList<>();
        
        for (Instructor instructor : instructors) {
            boolean isAvailable = isInstructorAvailable(instructor);
            MatchResultResponse.InstructorMatchDetail detail = calculateMatchScore(course, instructor, config, isAvailable);
            details.add(detail);
        }
        
        details.sort((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()));
        
        int rank = 1;
        for (MatchResultResponse.InstructorMatchDetail detail : details) {
            detail.setRank(rank++);
        }
        
        Double bestScore = details.isEmpty() ? 0.0 : details.get(0).getTotalScore();
        boolean hasGap = bestScore < config.getGapThreshold() || details.isEmpty();
        
        MatchRecord record = saveMatchRecord(course, config, details, hasGap, bestScore, false, null);
        saveMatchRecordDetails(record.getId(), details);
        
        operationLogService.log("MATCH", "COURSE", course.getId(), 
                String.format("课程匹配推演：%s，最佳匹配度：%.2f", course.getName(), bestScore));
        
        return buildResponse(record, details);
    }

    @Override
    @Transactional
    public List<MatchResultResponse> batchMatch(BatchMatchRequest request) {
        List<Course> courses = courseRepository.findByIndustry(request.getIndustry());
        List<MatchResultResponse> results = new ArrayList<>();
        String batchId = UUID.randomUUID().toString().substring(0, 8);
        
        for (Course course : courses) {
            MatchRequest matchRequest = new MatchRequest(course.getId(), request.getWeightConfigId());
            MatchResultResponse result = matchInstructors(matchRequest);
            MatchRecord record = matchRecordRepository.findById(result.getRecordId()).orElse(null);
            if (record != null) {
                record.setBatchMatch(true);
                record.setBatchIndustry(request.getIndustry());
                record.setBatchId(batchId);
                matchRecordRepository.save(record);
            }
            results.add(result);
        }
        
        operationLogService.log("BATCH_MATCH", "INDUSTRY", null, 
                String.format("行业批量匹配：%s，课程数量：%d", request.getIndustry(), courses.size()));
        
        return results;
    }

    @Override
    public List<MatchRecord> getAllMatchRecords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return matchRecordRepository.findAllByOrderByCreatedAtDesc(pageable).getContent();
    }

    @Override
    public MatchRecord getMatchRecordById(Long id) {
        return matchRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("匹配记录不存在: " + id));
    }

    @Override
    public List<MatchRecordDetail> getMatchRecordDetails(Long recordId) {
        return matchRecordDetailRepository.findByRecordIdOrderByRankAsc(recordId);
    }

    @Override
    public List<GapWarningResponse> getGapWarnings() {
        List<MatchRecord> gapRecords = matchRecordRepository.findByHasGapTrue();
        Map<Long, GapWarningResponse> warnings = new LinkedHashMap<>();
        
        for (MatchRecord record : gapRecords) {
            if (!warnings.containsKey(record.getCourseId())) {
                Course course = courseRepository.findById(record.getCourseId()).orElse(null);
                if (course != null) {
                    GapWarningResponse warning = new GapWarningResponse();
                    warning.setCourseId(course.getId());
                    warning.setCourseName(course.getName());
                    warning.setIndustry(course.getIndustry());
                    warning.setDifficulty(course.getDifficulty());
                    warning.setBestScore(record.getBestScore());
                    warning.setMatchCount(record.getMatchCount());
                    warning.setLastMatchTime(record.getCreatedAt());
                    
                    String level = record.getBestScore() < 30 ? "HIGH" : 
                                  record.getBestScore() < 50 ? "MEDIUM" : "LOW";
                    warning.setWarningLevel(level);
                    
                    warnings.put(course.getId(), warning);
                }
            }
        }
        
        warnings.values().stream()
                .sorted((a, b) -> b.getWarningLevel().compareTo(a.getWarningLevel()))
                .collect(Collectors.toList());
        
        return new ArrayList<>(warnings.values());
    }

    @Override
    @Transactional
    public void deleteMatchRecord(Long id) {
        MatchRecord record = matchRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("匹配记录不存在: " + id));
        
        matchRecordDetailRepository.deleteByRecordId(id);
        matchRecordRepository.deleteById(id);
        
        operationLogService.log("DELETE", "MATCH_RECORD", id, "删除匹配记录");
    }

    @Override
    public List<MatchRecord> getMatchRecordsByCourseId(Long courseId) {
        return matchRecordRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    @Override
    public List<MatchRecord> getMatchRecordsByIndustry(String industry) {
        return matchRecordRepository.findByCourseIndustryOrderByCreatedAtDesc(industry);
    }

    private MatchResultResponse.InstructorMatchDetail calculateMatchScore(Course course, Instructor instructor, WeightConfig config, boolean isAvailable) {
        MatchResultResponse.InstructorMatchDetail detail = new MatchResultResponse.InstructorMatchDetail();
        detail.setInstructorId(instructor.getId());
        detail.setInstructorName(instructor.getName());
        
        double teachingYearsScore = calculateTeachingYearsScore(instructor.getTeachingYears());
        double qualificationScore = skillService.calculateSkillMatchScore(course.getId(), instructor.getId());
        double ratingScore = calculateRatingScore(instructor.getStudentRating());
        double expertiseScore = calculateExpertiseScore(course.getIndustry(), instructor.getExpertiseAreas());
        double styleScore = calculateStyleScore(course.getStyleRequirements(), instructor.getTeachingStyle());
        
        detail.setTeachingYearsScore(teachingYearsScore);
        detail.setQualificationScore(qualificationScore);
        detail.setRatingScore(ratingScore);
        detail.setExpertiseScore(expertiseScore);
        detail.setStyleScore(styleScore);
        
        double totalScore = teachingYearsScore * config.getTeachingYearsWeight()
                + qualificationScore * config.getQualificationWeight()
                + ratingScore * config.getRatingWeight()
                + expertiseScore * config.getExpertiseWeight()
                + styleScore * config.getStyleWeight();
        
        double availabilityPenalty = isAvailable ? 0 : 30;
        totalScore = Math.max(totalScore - availabilityPenalty, 0);
        
        detail.setTotalScore(Math.round(totalScore * 100.0) / 100.0);
        detail.setAvailable(isAvailable);
        return detail;
    }
    
    private boolean isInstructorAvailable(Instructor instructor) {
        if (!instructor.getAvailable()) {
            return false;
        }
        
        int currentMonthCourses = assignmentRepository.countByInstructorIdAndStatusAndEndDateAfter(
                instructor.getId(), "CONFIRMED", java.time.LocalDate.now().withDayOfMonth(1));
        
        return currentMonthCourses < instructor.getMaxCoursesPerMonth();
    }

    private double calculateTeachingYearsScore(int years) {
        if (years >= 15) return 100.0;
        if (years >= 10) return 85.0;
        if (years >= 5) return 70.0;
        if (years >= 3) return 55.0;
        if (years >= 1) return 40.0;
        return 20.0;
    }

    private double calculateRatingScore(Double rating) {
        if (rating == null) return 0.0;
        return Math.min(rating / 5.0 * 100, 100.0);
    }

    private double calculateExpertiseScore(String industry, String expertise) {
        if (industry == null || industry.isEmpty()) return 100.0;
        if (expertise == null || expertise.isEmpty()) return 0.0;
        
        String[] expertiseList = expertise.split(",");
        int matchCount = 0;
        
        for (String exp : expertiseList) {
            if (industry.contains(exp.trim()) || exp.trim().contains(industry)) {
                matchCount++;
            }
        }
        
        if (matchCount == 0) return 20.0;
        return (double) matchCount / expertiseList.length * 100;
    }

    private double calculateStyleScore(String styleRequirements, String teachingStyle) {
        if (styleRequirements == null || styleRequirements.isEmpty()) return 100.0;
        if (teachingStyle == null || teachingStyle.isEmpty()) return 30.0;
        
        String[] styles = styleRequirements.split(",");
        int matchCount = 0;
        
        for (String style : styles) {
            if (teachingStyle.contains(style.trim())) {
                matchCount++;
            }
        }
        
        if (matchCount == 0) return 30.0;
        return 30.0 + (double) matchCount / styles.length * 70;
    }

    private MatchRecord saveMatchRecord(Course course, WeightConfig config, 
            List<MatchResultResponse.InstructorMatchDetail> details, 
            boolean hasGap, Double bestScore, boolean batchMatch, String batchIndustry) {
        
        MatchRecord record = MatchRecord.builder()
                .courseId(course.getId())
                .courseName(course.getName())
                .courseIndustry(course.getIndustry())
                .weightConfigId(config.getId())
                .teachingYearsWeight(config.getTeachingYearsWeight())
                .qualificationWeight(config.getQualificationWeight())
                .ratingWeight(config.getRatingWeight())
                .expertiseWeight(config.getExpertiseWeight())
                .styleWeight(config.getStyleWeight())
                .hasGap(hasGap)
                .bestScore(bestScore)
                .matchCount(details.size())
                .batchMatch(batchMatch)
                .batchIndustry(batchIndustry)
                .operator("system")
                .algorithmVersion("1.0")
                .build();
        
        return matchRecordRepository.save(record);
    }

    private void saveMatchRecordDetails(Long recordId, List<MatchResultResponse.InstructorMatchDetail> details) {
        for (MatchResultResponse.InstructorMatchDetail detail : details) {
            MatchRecordDetail recordDetail = new MatchRecordDetail();
            recordDetail.setRecordId(recordId);
            recordDetail.setInstructorId(detail.getInstructorId());
            recordDetail.setInstructorName(detail.getInstructorName());
            recordDetail.setTotalScore(detail.getTotalScore());
            recordDetail.setTeachingYearsScore(detail.getTeachingYearsScore());
            recordDetail.setQualificationScore(detail.getQualificationScore());
            recordDetail.setRatingScore(detail.getRatingScore());
            recordDetail.setExpertiseScore(detail.getExpertiseScore());
            recordDetail.setStyleScore(detail.getStyleScore());
            recordDetail.setRank(detail.getRank());
            matchRecordDetailRepository.save(recordDetail);
        }
    }

    private MatchResultResponse buildResponse(MatchRecord record, List<MatchResultResponse.InstructorMatchDetail> details) {
        MatchResultResponse response = new MatchResultResponse();
        response.setRecordId(record.getId());
        response.setCourseName(record.getCourseName());
        response.setCourseIndustry(record.getCourseIndustry());
        response.setHasGap(record.getHasGap());
        response.setBestScore(record.getBestScore());
        response.setMatchCount(record.getMatchCount());
        response.setInstructors(details);
        return response;
    }

    private WeightConfig getDefaultConfig() {
        return weightConfigRepository.findByIsDefaultTrue().stream()
                .findFirst()
                .orElseGet(() -> weightConfigRepository.findByActiveTrue().stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未配置权重方案")));
    }
}