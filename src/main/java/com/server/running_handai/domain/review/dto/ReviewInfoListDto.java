package com.server.running_handai.domain.review.dto;

import java.util.List;

public record ReviewInfoListDto(
        double starAverage,
        int reviewCount,
        List<ReviewInfoDto> reviewInfoDtos
) {
    public static ReviewInfoListDto from(double starAverage, List<ReviewInfoDto> reviewInfoDtos) {
        return new ReviewInfoListDto(starAverage, reviewInfoDtos.size(), reviewInfoDtos);
    }
}
