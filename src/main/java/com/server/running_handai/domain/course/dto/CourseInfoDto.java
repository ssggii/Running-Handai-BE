package com.server.running_handai.domain.course.dto;

public interface CourseInfoDto {
    long getId();
    String getName();
    String getThumbnailUrl();
    double getDistance();
    int getDuration();
    double getMaxElevation();
    double getDistanceFromUser(); // 코스 시작점과 사용자의 거리
}
