package com.server.running_handai.domain.course.event;

import static com.server.running_handai.domain.course.entity.SpotStatus.*;
import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.spot.service.SpotDataService;
import com.server.running_handai.global.response.exception.BusinessException;
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
    private final CourseRepository courseRepository;

    /**
     * CourseCreatedEvent를 수신하여 비동기적으로 즐길거리를 초기화하고,
     * Course의 즐길거리 초기화 상태를 업데이트합니다.
     *
     * @param event 코스 생성 이벤트 객체
     */
    @Async
    @TransactionalEventListener
    public void handleCourseCreatedEvent(CourseCreatedEvent event) {
        log.info("[이벤트 수신] 코스 생성 이벤트 수신. courseId: {}", event.courseId());
        Course course = courseRepository.findById(event.courseId()).orElseThrow(() ->
                new BusinessException(COURSE_NOT_FOUND));

        if (!event.isInsideBusan()) {
            log.info("부산 외 코스이므로 초기화를 생략합니다. courseId: {}", event.courseId());
            course.updateSpotStatus(NOT_APPLICABLE);
            courseRepository.save(course);
            return;
        }

        try {
            log.info("부산 내 코스. 즐길거리 초기화 작업을 시작합니다. courseId: {}", event.courseId());
            course.updateSpotStatus(IN_PROGRESS);
            courseRepository.save(course);
            log.info("코스 상태를 IN_PROGRESS로 변경했습니다. courseId: {}", event.courseId());
        } catch (Exception e) {
            log.error("코스 상태를 IN_PROGRESS로 변경 중 오류 발생. courseId: {}", event.courseId(), e);
            course.updateSpotStatus(FAILED); // 상태 변경부터 실패했다면 FAILED로 처리하고 작업 종료
            courseRepository.save(course);
            return;
        }

        try {
            spotDataService.updateSpots(event.courseId());
            course.updateSpotStatus(COMPLETED);
            courseRepository.save(course);
            log.info("비동기 즐길거리 초기화 작업을 완료했습니다. courseId: {}", event.courseId());
        } catch (Exception e) {
            log.error("비동기 즐길거리 초기화 작업 중 오류 발생. courseId: {}", event.courseId(), e);
            course.updateSpotStatus(FAILED);
            courseRepository.save(course);
        }
    }

}
