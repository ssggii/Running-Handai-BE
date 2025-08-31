package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;

import java.util.List;

public record MyCourseDetailDto(
        long courseId,
        String name,
        double distance,
        int duration,
        double maxElevation,
        double minElevation,
        List<TrackPointDto> trackPoints
) {
    public static MyCourseDetailDto from(Course course, List<TrackPointDto> trackPoints) {
        return new MyCourseDetailDto(
                course.getId(),
                course.getName(),
                course.getDistance(),
                course.getDuration(),
                course.getMaxElevation(),
                course.getMinElevation(),
                trackPoints
        );
    }
}