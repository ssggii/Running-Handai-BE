package com.server.running_handai.domain.spot.dto;

import java.util.List;

public record SpotDetailDto (
    long courseId,
    int spotCount,
    List<SpotInfoDto> spots
) {
}