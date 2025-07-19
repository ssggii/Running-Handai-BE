package com.server.running_handai.domain.course.dto;

public interface CourseInfoDto {
    Long getId();
    String getThumbnailUrl();
    Integer getDistance();
    Integer getDuration();
    Integer getMaxElevation();
    double getDistanceFromUser(); // 코스 시작점과 사용자의 거리
}
