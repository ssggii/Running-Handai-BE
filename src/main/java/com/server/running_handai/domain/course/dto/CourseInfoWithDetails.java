package com.server.running_handai.domain.course.dto;

import java.util.List;

public record CourseInfoWithDetails(
        long courseId,
        String thumbnailUrl,
        int distance,
        int duration,
        int maxElevation,
        double distanceFromUser,
        int bookmarks,
        boolean isBookmarked,
        List<TrackPointDto> trackPoints
) {
}
