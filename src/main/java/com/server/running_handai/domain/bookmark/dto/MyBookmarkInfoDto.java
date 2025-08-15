package com.server.running_handai.domain.bookmark.dto;

public record MyBookmarkInfoDto(
        String courseThumbnailUrl,
        int bookmarkCount,
        boolean isBookmarked
) {
    public static MyBookmarkInfoDto from(String courseThumbnailUrl, int bookmarkCount, boolean isBookmarked) {
        return new MyBookmarkInfoDto(courseThumbnailUrl, bookmarkCount, isBookmarked);
    }
}
