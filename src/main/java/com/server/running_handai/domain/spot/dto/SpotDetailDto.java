package com.server.running_handai.domain.spot.dto;

import java.util.List;

public record SpotDetailDto (
    long courseId,
    int spotCount,
    List<SpotInfoDto> spots
) {
    public static SpotDetailDto from(long courseId, List<SpotInfoDto> spots) {
        return new SpotDetailDto(
                courseId,
                spots.size(),
                spots
        );
    }
}