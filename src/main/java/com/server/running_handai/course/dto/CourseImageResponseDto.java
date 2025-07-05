package com.server.running_handai.course.dto;

import com.server.running_handai.course.entity.Course;
import com.server.running_handai.course.entity.CourseImage;
import lombok.Data;

@Data
public class CourseImageResponseDto {
    private Long courseId;
    private String courseName;
    private String courseImage;

    public CourseImageResponseDto(Course course, CourseImage courseImage) {
        this.courseId = course.getId();
        this.courseName = course.getName();
        this.courseImage = courseImage.getImgUrl();
    }
}
