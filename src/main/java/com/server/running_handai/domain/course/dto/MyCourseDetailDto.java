package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;

import java.util.List;

public record MyCourseDetailDto(
        long courseId,
        String name,
        int distance,
        int duration,
        int maxElevation,
        int minElevation,
        List<TrackPointDto> trackPoints
) {
    public static MyCourseDetailDto from(Course course, List<TrackPointDto> trackPoints) {
        return new MyCourseDetailDto(
                course.getId(),
                course.getName(),
                (int) Math.round(course.getDistance()),
                course.getDuration(),
                (int) Math.round(course.getMaxElevation()),
                (int) Math.round(course.getMinElevation()),
                trackPoints
        );
    }
}