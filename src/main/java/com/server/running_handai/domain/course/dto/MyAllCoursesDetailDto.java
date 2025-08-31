package com.server.running_handai.domain.course.dto;

import java.util.List;

public record MyAllCoursesDetailDto(
        int courseCount,
        List<CourseInfoDto> courses
) {
    public static MyAllCoursesDetailDto from(List<CourseInfoDto> courseInfoDtos) {
        return new MyAllCoursesDetailDto(
                courseInfoDtos.size(),
                courseInfoDtos
        );
    }
}