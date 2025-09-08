package com.server.running_handai.domain.course.dto;

import java.util.List;

public record MyAllCoursesDetailDto(
        int courseCount,
        List<MyCourseInfoDto> courses
) {
    public static MyAllCoursesDetailDto from(List<MyCourseInfoDto> courseInfoDtos) {
        return new MyAllCoursesDetailDto(
                courseInfoDtos.size(),
                courseInfoDtos
        );
    }
}
