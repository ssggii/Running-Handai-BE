package com.server.running_handai.domain.course.event;

import com.server.running_handai.domain.spot.service.SpotDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventListener {

    private final SpotDataService spotDataService;

    /**
     * CourseCreatedEvent를 수신하여 비동기적으로 즐길거리를 초기화합니다.
     *
     * @param event 코스 생성 이벤트 객체
     */
    @Async
    @TransactionalEventListener
    public void handleCourseCreatedEvent(CourseCreatedEvent event) {
        log.info("[이벤트 수신] 코스 생성 이벤트 수신. courseId: {}", event.courseId());

        // 부산 외 코스는 즐길거리 초기화 생략
        if (!event.isInsideBusan()) {
            log.info("부산 외 코스이므로 초기화를 생략합니다. courseId: {}", event.courseId());
            return;
        }

        // 부산 내 코스는 즐길거리 초기화 진행
        try {
            log.info("부산 내 코스이므로 비동기 즐길거리 초기화 작업을 시작합니다. courseId: {}", event.courseId());
            spotDataService.updateSpots(event.courseId());
            log.info("비동기 즐길거리 초기화 작업을 완료했습니다. courseId: {}", event.courseId());
        } catch (Exception e) {
            log.error("비동기 즐길거리 초기화 작업 중 오류 발생. courseId: {}", event.courseId(), e);
        }
    }

}
