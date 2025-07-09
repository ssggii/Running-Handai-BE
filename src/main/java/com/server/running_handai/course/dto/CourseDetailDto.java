package com.server.running_handai.course.dto;

import java.util.List;

public record CourseDetailDto(
        Long courseId,
        int distance,
        int duration,
        Double minElevation,
        Double maxElevation,
        String level,
        List<String> roadConditions,
        List<TrackPointDto> trackPoints
) {
}
