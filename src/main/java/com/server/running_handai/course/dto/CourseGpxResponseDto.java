package com.server.running_handai.course.dto;

import com.server.running_handai.course.entity.Course;
import lombok.Data;

@Data
public class CourseGpxResponseDto {
    private Long courseId;
    private String courseName;
    private String gpxPath;

    public CourseGpxResponseDto(Course course) {
        this.courseId = course.getId();
        this.courseName = course.getName();
        this.gpxPath = course.getGpxPath();
    }
}
