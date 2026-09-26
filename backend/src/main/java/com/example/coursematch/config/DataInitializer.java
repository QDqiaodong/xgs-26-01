package com.example.coursematch.config;

import com.example.coursematch.entity.Course;
import com.example.coursematch.entity.GapWarningRule;
import com.example.coursematch.entity.Instructor;
import com.example.coursematch.entity.Role;
import com.example.coursematch.entity.User;
import com.example.coursematch.entity.WeightConfig;
import com.example.coursematch.repository.CourseRepository;
import com.example.coursematch.repository.GapWarningRuleRepository;
import com.example.coursematch.repository.InstructorRepository;
import com.example.coursematch.repository.RoleRepository;
import com.example.coursematch.repository.UserRepository;
import com.example.coursematch.repository.WeightConfigRepository;
import com.example.coursematch.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
    private final WeightConfigRepository weightConfigRepository;
    private final GapWarningRuleRepository gapWarningRuleRepository;
    private final SkillService skillService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() == 0) {
            initRoles();
        }
        
        if (userRepository.count() == 0) {
            initUsers();
        }
        
        if (weightConfigRepository.count() == 0) {
            initWeightConfigs();
        }
        
        if (courseRepository.count() == 0) {
            initCourses();
        }
        
        if (instructorRepository.count() == 0) {
            initInstructors();
        }
        
        if (gapWarningRuleRepository.count() == 0) {
            initGapWarningRules();
        }
        
        if (courseRepository.count() > 0) {
            initCourseSkills();
        }
        
        if (instructorRepository.count() > 0) {
            initInstructorSkills();
        }
    }
    
    private void initRoles() {
        Role adminRole = Role.builder()
                .name("ADMIN")
                .description("管理员，拥有全部权限")
                .active(true)
                .build();
        roleRepository.save(adminRole);

        Role courseAdminRole = Role.builder()
                .name("COURSE_ADMIN")
                .description("课程管理员，负责课程管理和匹配推演")
                .active(true)
                .build();
        roleRepository.save(courseAdminRole);

        Role instructorAdminRole = Role.builder()
                .name("INSTRUCTOR_ADMIN")
                .description("讲师管理员，负责讲师档案管理")
                .active(true)
                .build();
        roleRepository.save(instructorAdminRole);

        Role viewerRole = Role.builder()
                .name("VIEWER")
                .description("查看者，只读权限")
                .active(true)
                .build();
        roleRepository.save(viewerRole);
    }
    
    private void initUsers() {
        Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        Role courseAdminRole = roleRepository.findByName("COURSE_ADMIN").orElse(null);
        Role instructorAdminRole = roleRepository.findByName("INSTRUCTOR_ADMIN").orElse(null);
        Role viewerRole = roleRepository.findByName("VIEWER").orElse(null);
        
        Set<Role> adminRoles = new HashSet<>();
        if (adminRole != null) adminRoles.add(adminRole);
        
        Set<Role> courseAdminRoles = new HashSet<>();
        if (courseAdminRole != null) courseAdminRoles.add(courseAdminRole);
        
        Set<Role> instructorAdminRoles = new HashSet<>();
        if (instructorAdminRole != null) instructorAdminRoles.add(instructorAdminRole);
        
        Set<Role> viewerRoles = new HashSet<>();
        if (viewerRole != null) viewerRoles.add(viewerRole);
        
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .realName("系统管理员")
                .phone("13800138000")
                .email("admin@example.com")
                .enabled(true)
                .roles(adminRoles)
                .createdBy("system")
                .build();
        userRepository.save(admin);
        
        User courseAdmin = User.builder()
                .username("course_admin")
                .password(passwordEncoder.encode("admin123"))
                .realName("课程管理员")
                .phone("13800138009")
                .email("course@example.com")
                .enabled(true)
                .roles(courseAdminRoles)
                .createdBy("system")
                .build();
        userRepository.save(courseAdmin);
        
        User instructorAdmin = User.builder()
                .username("instructor_admin")
                .password(passwordEncoder.encode("admin123"))
                .realName("讲师管理员")
                .phone("13800138010")
                .email("instructor@example.com")
                .enabled(true)
                .roles(instructorAdminRoles)
                .createdBy("system")
                .build();
        userRepository.save(instructorAdmin);
        
        User viewer = User.builder()
                .username("viewer")
                .password(passwordEncoder.encode("admin123"))
                .realName("查看用户")
                .phone("13800138011")
                .email("viewer@example.com")
                .enabled(true)
                .roles(viewerRoles)
                .createdBy("system")
                .build();
        userRepository.save(viewer);
    }

    private void initWeightConfigs() {
        WeightConfig defaultConfig = WeightConfig.builder()
                .name("默认权重配置")
                .description("系统默认的五维度权重配置")
                .teachingYearsWeight(0.2)
                .qualificationWeight(0.25)
                .ratingWeight(0.2)
                .expertiseWeight(0.2)
                .styleWeight(0.15)
                .gapThreshold(50.0)
                .isDefault(true)
                .active(true)
                .createdBy("system")
                .build();
        weightConfigRepository.save(defaultConfig);

        WeightConfig qualityFocus = WeightConfig.builder()
                .name("质量导向配置")
                .description("注重专业资质和学员口碑的权重配置")
                .teachingYearsWeight(0.15)
                .qualificationWeight(0.3)
                .ratingWeight(0.3)
                .expertiseWeight(0.15)
                .styleWeight(0.1)
                .gapThreshold(60.0)
                .isDefault(false)
                .active(true)
                .createdBy("system")
                .build();
        weightConfigRepository.save(qualityFocus);

        WeightConfig expertiseFocus = WeightConfig.builder()
                .name("专业导向配置")
                .description("注重行业擅长和授课年限的权重配置")
                .teachingYearsWeight(0.25)
                .qualificationWeight(0.15)
                .ratingWeight(0.15)
                .expertiseWeight(0.3)
                .styleWeight(0.15)
                .gapThreshold(55.0)
                .isDefault(false)
                .active(true)
                .createdBy("system")
                .build();
        weightConfigRepository.save(expertiseFocus);
    }

    private void initCourses() {
        Course c1 = Course.builder()
                .name("Java高级开发实战")
                .industry("IT互联网")
                .difficulty("高级")
                .requiredQualifications("Java认证,Oracle认证")
                .styleRequirements("深入浅出,实战导向")
                .description("深入讲解Java高级特性和企业级应用开发")
                .status("ACTIVE")
                .totalHours(40)
                .maxStudents(30)
                .createdBy("system")
                .build();
        courseRepository.save(c1);

        Course c2 = Course.builder()
                .name("前端Vue3开发")
                .industry("IT互联网")
                .difficulty("中级")
                .requiredQualifications("前端开发认证")
                .styleRequirements("互动教学,案例驱动")
                .description("掌握Vue3核心技术和现代前端开发流程")
                .status("ACTIVE")
                .totalHours(32)
                .maxStudents(25)
                .createdBy("system")
                .build();
        courseRepository.save(c2);

        Course c3 = Course.builder()
                .name("金融数据分析")
                .industry("金融")
                .difficulty("高级")
                .requiredQualifications("CFA证书,Python认证")
                .styleRequirements("理论结合实践")
                .description("运用数据分析技术解决金融领域实际问题")
                .status("ACTIVE")
                .totalHours(48)
                .maxStudents(20)
                .createdBy("system")
                .build();
        courseRepository.save(c3);

        Course c4 = Course.builder()
                .name("企业财务管理")
                .industry("金融")
                .difficulty("中级")
                .requiredQualifications("CPA证书")
                .styleRequirements("案例教学")
                .description("企业财务管理的核心知识和实践技能")
                .status("ACTIVE")
                .totalHours(36)
                .maxStudents(30)
                .createdBy("system")
                .build();
        courseRepository.save(c4);

        Course c5 = Course.builder()
                .name("医疗信息化建设")
                .industry("医疗健康")
                .difficulty("高级")
                .requiredQualifications("医疗信息化认证,项目管理认证")
                .styleRequirements("系统讲解,实操演示")
                .description("医疗行业信息化建设的规划和实施")
                .status("ACTIVE")
                .totalHours(40)
                .maxStudents(25)
                .createdBy("system")
                .build();
        courseRepository.save(c5);

        Course c6 = Course.builder()
                .name("医疗器械管理")
                .industry("医疗健康")
                .difficulty("初级")
                .requiredQualifications("医疗器械上岗证")
                .styleRequirements("通俗易懂")
                .description("医疗器械的日常管理和维护")
                .status("ACTIVE")
                .totalHours(24)
                .maxStudents(35)
                .createdBy("system")
                .build();
        courseRepository.save(c6);

        Course c7 = Course.builder()
                .name("智能制造技术")
                .industry("制造业")
                .difficulty("高级")
                .requiredQualifications("自动化认证,PLC认证")
                .styleRequirements("现场演示,实操教学")
                .description("智能制造技术原理和应用实践")
                .status("ACTIVE")
                .totalHours(48)
                .maxStudents(20)
                .createdBy("system")
                .build();
        courseRepository.save(c7);

        Course c8 = Course.builder()
                .name("供应链管理")
                .industry("物流")
                .difficulty("中级")
                .requiredQualifications("供应链管理师")
                .styleRequirements("案例分析,互动讨论")
                .description("供应链管理的核心理论和实践方法")
                .status("ACTIVE")
                .totalHours(32)
                .maxStudents(30)
                .createdBy("system")
                .build();
        courseRepository.save(c8);
    }

    private void initInstructors() {
        Instructor i1 = Instructor.builder()
                .name("张明")
                .phone("13800138001")
                .email("zhangming@example.com")
                .certificates("Java认证,Oracle认证,高级软件工程师")
                .teachingYears(15)
                .studentRating(4.8)
                .expertiseAreas("Java,IT互联网,企业级开发")
                .teachingStyle("深入浅出,实战导向")
                .resume("15年Java开发和教学经验，曾在知名互联网公司担任技术总监")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i1);

        Instructor i2 = Instructor.builder()
                .name("李华")
                .phone("13800138002")
                .email("lihua@example.com")
                .certificates("前端开发认证,Vue认证")
                .teachingYears(8)
                .studentRating(4.6)
                .expertiseAreas("前端开发,IT互联网,Vue,React")
                .teachingStyle("互动教学,案例驱动")
                .resume("8年前端开发经验，参与多个大型项目的前端架构设计")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i2);

        Instructor i3 = Instructor.builder()
                .name("王芳")
                .phone("13800138003")
                .email("wangfang@example.com")
                .certificates("CFA证书,CPA证书")
                .teachingYears(12)
                .studentRating(4.9)
                .expertiseAreas("金融,数据分析,投资分析")
                .teachingStyle("理论结合实践")
                .resume("12年金融行业经验，曾任某证券公司投资总监")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i3);

        Instructor i4 = Instructor.builder()
                .name("陈伟")
                .phone("13800138004")
                .email("chenwei@example.com")
                .certificates("CPA证书,税务师")
                .teachingYears(10)
                .studentRating(4.7)
                .expertiseAreas("金融,财务管理,税务筹划")
                .teachingStyle("案例教学")
                .resume("10年财务工作经验，曾在四大会计师事务所任职")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i4);

        Instructor i5 = Instructor.builder()
                .name("刘洋")
                .phone("13800138005")
                .email("liuyang@example.com")
                .certificates("医疗信息化认证,项目管理认证,PMP")
                .teachingYears(11)
                .studentRating(4.5)
                .expertiseAreas("医疗健康,信息化建设,项目管理")
                .teachingStyle("系统讲解,实操演示")
                .resume("11年医疗信息化行业经验，主导多个大型医院信息化项目")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i5);

        Instructor i6 = Instructor.builder()
                .name("赵敏")
                .phone("13800138006")
                .email("zhaomin@example.com")
                .certificates("自动化认证,PLC认证")
                .teachingYears(9)
                .studentRating(4.6)
                .expertiseAreas("制造业,智能制造,自动化")
                .teachingStyle("现场演示,实操教学")
                .resume("9年制造业自动化经验，精通PLC编程和工业控制")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i6);

        Instructor i7 = Instructor.builder()
                .name("孙磊")
                .phone("13800138007")
                .email("sunlei@example.com")
                .certificates("供应链管理师,物流师")
                .teachingYears(7)
                .studentRating(4.4)
                .expertiseAreas("物流,供应链管理,仓储管理")
                .teachingStyle("案例分析,互动讨论")
                .resume("7年物流行业经验，曾在知名电商企业负责供应链管理")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i7);

        Instructor i8 = Instructor.builder()
                .name("周婷")
                .phone("13800138008")
                .email("zhouting@example.com")
                .certificates("医疗器械上岗证")
                .teachingYears(5)
                .studentRating(4.3)
                .expertiseAreas("医疗健康,医疗器械")
                .teachingStyle("通俗易懂")
                .resume("5年医疗器械行业经验，熟悉各类医疗器械的使用和管理")
                .status("ACTIVE")
                .available(true)
                .maxCoursesPerMonth(4)
                .currentMonthCourses(0)
                .createdBy("system")
                .build();
        instructorRepository.save(i8);
    }

    private void initGapWarningRules() {
        GapWarningRule rule1 = GapWarningRule.builder()
                .ruleName("高分缺口预警")
                .description("最佳匹配度低于30分的课程触发高风险预警")
                .warningLevel("HIGH")
                .scoreThreshold(30.0)
                .minMatchCount(1)
                .isActive(true)
                .createdBy("system")
                .build();
        gapWarningRuleRepository.save(rule1);

        GapWarningRule rule2 = GapWarningRule.builder()
                .ruleName("中分缺口预警")
                .description("最佳匹配度在30-50分之间的课程触发中风险预警")
                .warningLevel("MEDIUM")
                .scoreThreshold(50.0)
                .minMatchCount(1)
                .isActive(true)
                .createdBy("system")
                .build();
        gapWarningRuleRepository.save(rule2);

        GapWarningRule rule3 = GapWarningRule.builder()
                .ruleName("低分缺口预警")
                .description("最佳匹配度在50-60分之间的课程触发低风险预警")
                .warningLevel("LOW")
                .scoreThreshold(60.0)
                .minMatchCount(1)
                .isActive(true)
                .createdBy("system")
                .build();
        gapWarningRuleRepository.save(rule3);
    }

    private void initCourseSkills() {
        skillService.addCourseRequiredSkills(1L, Arrays.asList("Java认证", "Oracle认证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(2L, Arrays.asList("前端开发认证", "Vue认证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(3L, Arrays.asList("CFA证书", "Python认证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(4L, Arrays.asList("CPA证书"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(5L, Arrays.asList("医疗信息化认证", "项目管理认证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(6L, Arrays.asList("医疗器械上岗证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(7L, Arrays.asList("自动化认证", "PLC认证"), "QUALIFICATION", true);
        skillService.addCourseRequiredSkills(8L, Arrays.asList("供应链管理师"), "QUALIFICATION", true);
    }

    private void initInstructorSkills() {
        skillService.addInstructorSkills(1L, Arrays.asList("Java认证", "Oracle认证", "高级软件工程师"), "CERTIFICATE");
        skillService.addInstructorSkills(2L, Arrays.asList("前端开发认证", "Vue认证", "React认证"), "CERTIFICATE");
        skillService.addInstructorSkills(3L, Arrays.asList("CFA证书", "CPA证书", "金融分析师"), "CERTIFICATE");
        skillService.addInstructorSkills(4L, Arrays.asList("CPA证书", "税务师", "会计师"), "CERTIFICATE");
        skillService.addInstructorSkills(5L, Arrays.asList("医疗信息化认证", "项目管理认证", "PMP"), "CERTIFICATE");
        skillService.addInstructorSkills(6L, Arrays.asList("自动化认证", "PLC认证", "工业控制"), "CERTIFICATE");
        skillService.addInstructorSkills(7L, Arrays.asList("供应链管理师", "物流师", "仓储管理"), "CERTIFICATE");
        skillService.addInstructorSkills(8L, Arrays.asList("医疗器械上岗证", "医疗设备操作"), "CERTIFICATE");
    }
}