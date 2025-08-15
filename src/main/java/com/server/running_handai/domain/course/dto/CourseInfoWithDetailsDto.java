package com.server.running_handai.domain.course.dto;

import java.util.List;

public record CourseInfoWithDetailsDto(
        long courseId,
        String courseName,
        String thumbnailUrl,
        int distance,
        int duration,
        int maxElevation,
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
                (int) courseInfoDto.getDistance(),
                courseInfoDto.getDuration(),
                (int) courseInfoDto.getMaxElevation(),
                courseInfoDto.getDistanceFromUser(),
                bookmarks,
                isBookmarked,
                trackPoints
        );
    }
}
