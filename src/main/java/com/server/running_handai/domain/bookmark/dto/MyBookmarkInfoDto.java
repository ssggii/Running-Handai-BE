package com.server.running_handai.domain.bookmark.dto;

public record MyBookmarkInfoDto(
        long bookmarkId,
        long courseId,
        String courseName,
        String thumbnailUrl,
        int bookmarkCount,
        boolean isBookmarked
) {
    public static MyBookmarkInfoDto from(BookmarkedCourseInfoDto courseInfoDto) {
        return new MyBookmarkInfoDto(
                courseInfoDto.getBookmarkId(),
                courseInfoDto.getCourseId(),
                courseInfoDto.getCourseName(),
                courseInfoDto.getThumbnailUrl(),
                courseInfoDto.getBookmarkCount(),
                courseInfoDto.getIsBookmarked()
        );
    }
}
