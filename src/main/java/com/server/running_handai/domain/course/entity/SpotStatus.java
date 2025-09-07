package com.server.running_handai.domain.course.entity;

public enum SpotStatus {
    NOT_STARTED, // 초기화 시작 전 (코스 생성 직후)
    NOT_APPLICABLE, // 해당 없음 (부산 외 코스)
    IN_PROGRESS, // 초기화 진행 중
    COMPLETED, // 초기화 완료 (즐길거리 없는 경우도 가능)
    FAILED // 초기화 과정 중 에러 발생
}
