package com.example.coursematch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CourseMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseMatchApplication.class, args);
    }
}