package com.server.running_handai.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseLevel {
    EASY("쉬움"),
    MEDIUM("보통"),
    HARD("어려움");

    private final String description;
}
