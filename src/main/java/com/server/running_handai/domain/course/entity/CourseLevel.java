package com.server.running_handai.domain.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseLevel {
    EASY("쉬움", 1),
    MEDIUM("보통", 2),
    HARD("어려움", 3);

    private final String description;
    private final int levelNumber;

    // 두루누비 API의 난이도 응답값(문자열)을 enum으로 변환하는 메서드
    public static CourseLevel fromApiValue(String level) {
        if (level == null) {
            return MEDIUM; // 기본값
        }
        return switch (level) {
            case "1" -> EASY;
            case "2" -> MEDIUM;
            case "3" -> HARD;
            default -> throw new IllegalStateException("Unexpected CourseLevel value: " + level);
        };
    }
}
