package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.RoadCondition;
import java.util.List;

public record CourseDetailDto(
        Long courseId,
        int distance,
        int duration,
        Double minElevation,
        Double maxElevation,
        String level,
        int bookmarks,
        boolean isBookmarked,
        List<String> roadConditions,
        List<TrackPointDto> trackPoints
) {
    public static CourseDetailDto of(Course course, List<TrackPointDto> trackPoints, BookmarkInfo bookmarkInfo) {
        List<String> roadConditions = course.getRoadConditions().stream()
                .map(RoadCondition::getDescription).toList();

        return new CourseDetailDto(
                course.getId(),
                course.getDistance(),
                course.getDuration(),
                course.getMinElevation(),
                course.getMaxElevation(),
                course.getLevel().getDescription(),
                bookmarkInfo.totalCount(),
                bookmarkInfo.isBookmarked(),
                roadConditions,
                trackPoints
        );
    }
}
