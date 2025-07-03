package com.server.running_handai.course.dto;

import com.server.running_handai.course.entity.Course;
import lombok.Data;

import java.util.List;

@Data
public class RoadConditionResponseDto {
    private Long courseId;
    private String courseName;
    private List<String> roadCondition;

    public RoadConditionResponseDto(Course course, List<String> descriptions) {
        this.courseId = course.getId();
        this.courseName = course.getName();
        this.roadCondition = descriptions;
    }
}
