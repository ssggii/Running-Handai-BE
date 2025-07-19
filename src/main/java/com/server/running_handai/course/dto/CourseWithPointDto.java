package com.server.running_handai.course.dto;

import java.util.List;

public record CourseWithPointDto(
        long courseId,
        String thumbnailUrl,
        int distance,
        int duration,
        int maxElevation,
        double distanceFromUser,
        int bookmarks,
        List<TrackPointDto> trackPoints
) {
}
