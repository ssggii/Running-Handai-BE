package com.server.running_handai.domain.bookmark.dto;

import java.util.List;

public record MyBookmarkDetailDto(
        int courseCount,
        List<MyBookmarkInfoDto> courses
) {
    public static MyBookmarkDetailDto from(List<MyBookmarkInfoDto> bookmarkCourses) {
        return new MyBookmarkDetailDto(bookmarkCourses.size(), bookmarkCourses);
    }
}
