package com.example.coursematch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称长度不能超过100个字符")
    private String name;

    @NotBlank(message = "行业不能为空")
    @Size(max = 50, message = "行业长度不能超过50个字符")
    private String industry;

    @NotBlank(message = "难度不能为空")
    @Size(max = 20, message = "难度长度不能超过20个字符")
    private String difficulty;

    @Size(max = 500, message = "必备资质长度不能超过500个字符")
    private String requiredQualifications;

    @Size(max = 200, message = "风格要求长度不能超过200个字符")
    private String styleRequirements;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    private Integer totalHours;

    private Integer maxStudents;

    private String status = "DRAFT";
}