package com.server.running_handai.domain.spot.dto;

import com.server.running_handai.domain.spot.entity.Spot;

public record SpotInfoDto(
        long spotId,
        String name,
        String description,
        String imageUrl
) {
    public static SpotInfoDto from(Spot spot) {
        return new SpotInfoDto(
                spot.getId(),
                spot.getName(),
                spot.getDescription(),
                spot.getSpotImage() != null ? spot.getSpotImage().getImgUrl() : null
        );
    }
}
