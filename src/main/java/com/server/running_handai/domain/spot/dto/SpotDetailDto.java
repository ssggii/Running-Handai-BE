package com.server.running_handai.domain.spot.dto;

import com.server.running_handai.domain.course.entity.Course;
import java.util.List;

public record SpotDetailDto (
    long courseId,
    int spotCount,
    String spotStatus,
    List<SpotInfoDto> spots
) {
    public static SpotDetailDto from(Course course, List<SpotInfoDto> spots) {
        return new SpotDetailDto(
                course.getId(),
                spots.size(),
                course.getSpotStatus().name(),
                spots
        );
    }
}