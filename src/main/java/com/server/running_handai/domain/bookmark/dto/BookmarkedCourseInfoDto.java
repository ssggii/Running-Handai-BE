package com.server.running_handai.domain.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "bookmarkId",
        "courseId",
        "thumbnailUrl",
        "distance",
        "duration",
        "maxElevation",
        "isBookmarked",
        "bookmarkCount"
})
public interface BookmarkedCourseInfoDto {
    long getBookmarkId();
    long getCourseId();
    String getThumbnailUrl();

    @JsonIgnore
    double getRawDistance();

    default int getDistance() {
        return (int) getRawDistance();
    }

    int getDuration();

    @JsonIgnore
    double getRawMaxElevation(); // JPA 전용

    default int getMaxElevation() { // 클라이언트 전용
        return (int) getRawMaxElevation();
    }

    boolean getIsBookmarked();
    int getBookmarkCount();
}
