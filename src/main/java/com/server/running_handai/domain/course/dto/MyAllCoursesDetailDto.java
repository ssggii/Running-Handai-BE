package com.server.running_handai.domain.course.dto;

import java.util.List;

public record MyAllCoursesDetailDto(
        int myCourseCount,
        List<MyCourseInfoDto> courses
) {
    public static MyAllCoursesDetailDto from(List<MyCourseInfoDto> courseInfoDtos) {
        return new MyAllCoursesDetailDto(courseInfoDtos.size(), courseInfoDtos);
    }

    public static MyAllCoursesDetailDto from(int myCourseCount, List<MyCourseInfoDto> courseInfoDtos) {
        return new MyAllCoursesDetailDto(myCourseCount, courseInfoDtos);
    }
}