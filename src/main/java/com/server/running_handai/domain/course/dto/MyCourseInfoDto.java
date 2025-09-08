package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import java.time.format.DateTimeFormatter;

public record MyCourseInfoDto(
    long courseId,
    String courseName,
    String thumbnailUrl,
    int distance,
    int duration,
    int maxElevation,
    String createdAt
) {
    public static MyCourseInfoDto from(Course course) {
        String thumbnailUrl = (course.getCourseImage() != null && course.getCourseImage().getImgUrl() != null)
                ? course.getCourseImage().getImgUrl() : null;
        int roundedDistance = (int) Math.round(course.getDistance());
        int roundedMaxElevation = (int) Math.round(course.getMaxElevation());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedCreatedAt = course.getCreatedAt().format(formatter);

        return new MyCourseInfoDto(
                course.getId(),
                course.getName(),
                thumbnailUrl,
                roundedDistance,
                course.getDuration(),
                roundedMaxElevation,
                formattedCreatedAt
        );
    }
}
