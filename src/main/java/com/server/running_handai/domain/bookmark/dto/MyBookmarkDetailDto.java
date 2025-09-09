package com.server.running_handai.domain.bookmark.dto;

import java.util.List;

public record MyBookmarkDetailDto(
        int bookmarkCount,
        List<MyBookmarkInfoDto> courses
) {
    public static MyBookmarkDetailDto from(int bookmarkCount, List<MyBookmarkInfoDto> bookmarkCourses) {
        return new MyBookmarkDetailDto(bookmarkCount, bookmarkCourses);
    }
}
