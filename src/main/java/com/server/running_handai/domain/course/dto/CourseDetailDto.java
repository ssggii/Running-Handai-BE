package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.RoadCondition;
import java.util.List;

public record CourseDetailDto(
        Long courseId,
        String courseName,
        double distance,
        int duration,
        int minElevation,
        int maxElevation,
        String level,
        int bookmarks,
        boolean isBookmarked,
        List<String> roadConditions,
        List<TrackPointDto> trackPoints
) {
    public static CourseDetailDto from(Course course, List<TrackPointDto> trackPoints, BookmarkInfoDto bookmarkInfoDto) {
        List<String> roadConditions = course.getRoadConditions().stream()
                .map(RoadCondition::getDescription).toList();

        return new CourseDetailDto(
                course.getId(),
                course.getName(),
                course.getDistance(),
                course.getDuration(),
                (int) course.getMinElevation().doubleValue(),
                (int) course.getMaxElevation().doubleValue(),
                course.getLevel().getDescription(),
                bookmarkInfoDto.totalCount(),
                bookmarkInfoDto.isBookmarked(),
                roadConditions,
                trackPoints
        );
    }
}
