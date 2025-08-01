package com.server.running_handai.domain.course.dto;

import java.util.List;

public record CourseInfoWithDetailsDto(
        long courseId,
        String thumbnailUrl,
        int distance,
        int duration,
        double maxElevation,
        double distanceFromUser,
        int bookmarks,
        boolean isBookmarked,
        List<TrackPointDto> trackPoints
) {
}
