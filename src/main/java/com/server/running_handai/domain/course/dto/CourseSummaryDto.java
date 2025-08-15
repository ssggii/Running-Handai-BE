package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.dto.ReviewInfoListDto;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;

import java.util.List;

public record CourseSummaryDto(
        double distance,
        int duration,
        double maxElevation,
        int reviewCount,
        double starAverage,
        List<ReviewInfoDto> reviews,
        List<SpotInfoDto> spots
) {
    public static CourseSummaryDto from(Course course, int reviewCount, double starAverage,
                                        List<ReviewInfoDto> reviewInfoDtos, List<SpotInfoDto> spotInfoDtos) {

        return new CourseSummaryDto(
                course.getDistance(),
                course.getDuration(),
                course.getMaxElevation(),
                reviewCount,
                starAverage,
                reviewInfoDtos,
                spotInfoDtos
        );
    }
}
