package com.example.coursematch.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorCreateRequest {
    @NotBlank(message = "讲师姓名不能为空")
    @Size(max = 50, message = "讲师姓名长度不能超过50个字符")
    private String name;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @Size(max = 500, message = "证书长度不能超过500个字符")
    private String certificates;

    @NotNull(message = "授课年限不能为空")
    @Min(value = 0, message = "授课年限不能为负数")
    @Max(value = 50, message = "授课年限不能超过50年")
    private Integer teachingYears;

    @NotNull(message = "学员评分不能为空")
    @Min(value = 0, message = "学员评分不能小于0")
    @Max(value = 5, message = "学员评分不能大于5")
    private Double studentRating;

    @Size(max = 500, message = "擅长领域长度不能超过500个字符")
    private String expertiseAreas;

    @Size(max = 200, message = "授课风格长度不能超过200个字符")
    private String teachingStyle;

    @Size(max = 500, message = "简历长度不能超过500个字符")
    private String resume;
}