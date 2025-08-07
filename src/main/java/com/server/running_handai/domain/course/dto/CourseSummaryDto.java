package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.review.dto.ReviewInfoListDto;

public record CourseSummaryDto(
        double distance,
        int duration,
        double maxElevation,
        ReviewInfoListDto reviewInfoListDto //TODO 즐길거리 dto 추가
) {
    public static CourseSummaryDto from(Course course, ReviewInfoListDto reviewInfoListDto) {
        return new CourseSummaryDto(
                course.getDistance(),
                course.getDuration(),
                course.getMaxElevation(),
                reviewInfoListDto
        );
    }
}
