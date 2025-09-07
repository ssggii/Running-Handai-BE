package com.server.running_handai.domain.course.dto;

import java.util.List;

public record MyCourseDetailDto(
        int courseCount,
        List<CourseInfoDto> courses
) {
    public static MyCourseDetailDto from(List<CourseInfoDto> courseInfoDtos) {
        return new MyCourseDetailDto(
                courseInfoDtos.size(),
                courseInfoDtos
        );
    }
}
