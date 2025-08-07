package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import java.util.List;

public record CourseInfoWithDetailsDto(
        long courseId,
        String courseName,
        String thumbnailUrl,
        double distance,
        int duration,
        double maxElevation,
        double distanceFromUser,
        int bookmarks,
        boolean isBookmarked,
        List<TrackPointDto> trackPoints
) {
    public static CourseInfoWithDetailsDto from(CourseInfoDto courseInfoDto, List<TrackPointDto> trackPoints, int bookmarks, boolean isBookmarked) {
        return new CourseInfoWithDetailsDto(
                courseInfoDto.getId(),
                courseInfoDto.getName(),
                courseInfoDto.getThumbnailUrl(),
                courseInfoDto.getDistance(),
                courseInfoDto.getDuration(),
                courseInfoDto.getMaxElevation(),
                courseInfoDto.getDistanceFromUser(),
                bookmarks,
                isBookmarked,
                trackPoints
        );
    }
}
