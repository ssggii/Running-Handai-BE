package com.server.running_handai.domain.spot.dto;

public record SpotInfoDto(
        long spotId,
        String name,
        String description,
        String imageUrl
) {
}
