package com.server.running_handai.domain.bookmark.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "bookmarkId",
        "courseId",
        "courseName",
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
    String getCourseName();
    String getThumbnailUrl();

    @JsonIgnore
    double getRawDistance();

    default int getDistance() {
        return (int) Math.round(getRawDistance());
    }

    int getDuration();

    @JsonIgnore
    double getRawMaxElevation();

    default int getMaxElevation() { // 클라이언트 전용
        return (int) Math.round(getRawMaxElevation());
    }

    boolean getIsBookmarked();
    int getBookmarkCount();
}
