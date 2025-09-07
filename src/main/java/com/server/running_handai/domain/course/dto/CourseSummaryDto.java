package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;

import java.util.List;

public record CourseSummaryDto(
        int distance,
        int duration,
        int maxElevation,
        int reviewCount,
        double starAverage,
        List<ReviewInfoDto> reviews,
        String spotStatus,
        List<SpotInfoDto> spots
) {
    public static CourseSummaryDto from(Course course, int reviewCount, double starAverage,
                                        List<ReviewInfoDto> reviewInfoDtos, List<SpotInfoDto> spotInfoDtos) {
        return new CourseSummaryDto(
                (int) course.getDistance(),
                course.getDuration(),
                (int) course.getMaxElevation().doubleValue(),
                reviewCount,
                starAverage,
                reviewInfoDtos,
                course.getSpotStatus().name(),
                spotInfoDtos
        );
    }
}
