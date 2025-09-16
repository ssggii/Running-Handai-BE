package com.server.running_handai.domain.course.dto;

public record CourseTrackPointDto(
        long courseId,
        double startPointLat,
        double startPointLon,
        double endPointLat,
        double endPointLon
) {
}