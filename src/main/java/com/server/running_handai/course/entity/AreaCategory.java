package com.server.running_handai.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AreaCategory {
    SEA("바다"),
    DOWNTOWN("도심"),
    RIVERSIDE("강변");

    private final String description;
}
