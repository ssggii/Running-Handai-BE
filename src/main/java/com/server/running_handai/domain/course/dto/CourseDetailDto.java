package com.server.running_handai.domain.course.dto;

import java.util.List;

public record CourseDetailDto(
        Long courseId,
        int distance,
        int duration,
        Double minElevation,
        Double maxElevation,
        String level,
        int bookmarks,
        boolean isBookmarked,
        List<String> roadConditions,
        List<TrackPointDto> trackPoints
) {
}
