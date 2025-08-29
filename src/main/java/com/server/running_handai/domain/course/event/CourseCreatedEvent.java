package com.server.running_handai.domain.course.event;

/**
 * 회원이 만든 코스가 성공적으로 생성되고 트랜잭션이 커밋된 후 발행되는 이벤트
 *
 * @param courseId 생성된 코스의 ID
 * @param isInsideBusan 부산 지역 내 코스인지 여부
 */
public record CourseCreatedEvent(
        Long courseId,
        boolean isInsideBusan
) {
}
