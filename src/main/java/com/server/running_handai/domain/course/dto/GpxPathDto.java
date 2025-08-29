package com.server.running_handai.domain.course.dto;

public record GpxPathDto(
        Long courseId,
        String gpxPath
) {
    public static GpxPathDto from(Long courseId, String gpxPath) {
        return new GpxPathDto(
                courseId,
                gpxPath
        );
    }
}
