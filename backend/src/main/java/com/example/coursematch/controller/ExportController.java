package com.example.coursematch.controller;

import com.example.coursematch.entity.Course;
import com.example.coursematch.entity.Instructor;
import com.example.coursematch.entity.MatchRecord;
import com.example.coursematch.repository.CourseRepository;
import com.example.coursematch.repository.InstructorRepository;
import com.example.coursematch.repository.MatchRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "数据导出", description = "数据导出接口")
public class ExportController {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final MatchRecordRepository matchRecordRepository;

    @GetMapping("/courses")
    @Operation(summary = "导出课程列表", description = "导出课程数据为CSV格式")
    public ResponseEntity<byte[]> exportCourses(@RequestParam(required = false) String industry) {
        List<Course> courses = industry != null 
                ? courseRepository.findByIndustry(industry)
                : courseRepository.findAll();
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,课程名称,行业,难度,总课时,最大学员数,状态,创建时间\n");
        
        for (Course course : courses) {
            csv.append(String.format("%d,%s,%s,%s,%d,%d,%s,%s\n",
                    course.getId(),
                    escapeCsv(course.getName()),
                    course.getIndustry(),
                    course.getDifficulty(),
                    course.getTotalHours() != null ? course.getTotalHours() : 0,
                    course.getMaxStudents() != null ? course.getMaxStudents() : 0,
                    course.getStatus(),
                    formatDateTime(course.getCreatedAt())
            ));
        }
        
        return buildCsvResponse(csv.toString(), "courses");
    }

    @GetMapping("/instructors")
    @Operation(summary = "导出讲师列表", description = "导出讲师数据为CSV格式")
    public ResponseEntity<byte[]> exportInstructors(@RequestParam(required = false) String status) {
        List<Instructor> instructors = status != null 
                ? instructorRepository.findByStatus(status)
                : instructorRepository.findAll();
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,姓名,手机号,邮箱,教龄,学员评分,状态,可用状态,创建时间\n");
        
        for (Instructor instructor : instructors) {
            csv.append(String.format("%d,%s,%s,%s,%d,%.2f,%s,%s,%s\n",
                    instructor.getId(),
                    escapeCsv(instructor.getName()),
                    instructor.getPhone() != null ? instructor.getPhone() : "",
                    instructor.getEmail() != null ? instructor.getEmail() : "",
                    instructor.getTeachingYears(),
                    instructor.getStudentRating(),
                    instructor.getStatus(),
                    instructor.getAvailable() ? "可用" : "不可用",
                    formatDateTime(instructor.getCreatedAt())
            ));
        }
        
        return buildCsvResponse(csv.toString(), "instructors");
    }

    @GetMapping("/matches")
    @Operation(summary = "导出匹配记录", description = "导出匹配记录数据为CSV格式")
    public ResponseEntity<byte[]> exportMatches(@RequestParam(required = false) String industry) {
        List<MatchRecord> records = industry != null 
                ? matchRecordRepository.findByCourseIndustry(industry)
                : matchRecordRepository.findAll();
        
        StringBuilder csv = new StringBuilder();
        csv.append("ID,课程名称,行业,最佳分数,匹配人数,是否缺口,权重配置ID,创建时间\n");
        
        for (MatchRecord record : records) {
            csv.append(String.format("%d,%s,%s,%.2f,%d,%s,%d,%s\n",
                    record.getId(),
                    escapeCsv(record.getCourseName()),
                    record.getCourseIndustry(),
                    record.getBestScore() != null ? record.getBestScore() : 0,
                    record.getMatchCount() != null ? record.getMatchCount() : 0,
                    record.getHasGap() ? "是" : "否",
                    record.getWeightConfigId(),
                    formatDateTime(record.getCreatedAt())
            ));
        }
        
        return buildCsvResponse(csv.toString(), "matches");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private ResponseEntity<byte[]> buildCsvResponse(String content, String filename) {
        byte[] csvBytes = content.getBytes(StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", 
                new String((filename + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv")
                        .getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        headers.setContentLength(csvBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }
}