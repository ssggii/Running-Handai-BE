package com.server.running_handai.domain.review.dto;

import java.util.List;

public record ReviewInfoListDto(
        double starAverage,
        int reviewCount,
        List<ReviewInfoDto> reviewInfoDtoList
) {
    public static ReviewInfoListDto from(double starAverage, List<ReviewInfoDto> reviewInfoDtoList) {
        return new ReviewInfoListDto(starAverage, reviewInfoDtoList.size(), reviewInfoDtoList);
    }
}
