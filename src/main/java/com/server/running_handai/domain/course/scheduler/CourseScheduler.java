package com.server.running_handai.domain.course.scheduler;

import com.server.running_handai.domain.course.service.CourseDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseScheduler {

    private final CourseDataService courseDataService;

    /**
     * 매일 새벽 4시에 코스 데이터 동기화 작업을 실행합니다.
     * cron = "[초] [분] [시] [일] [월] [요일]"
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul") // 매일 새벽 4시 30분 0초
    public void scheduleDurunubiCourseSync() {
        log.info("[스케줄러] 두루누비 코스 동기화 작업을 시작합니다.");
        try {
            courseDataService.synchronizeCourseData();
            log.info("[스케줄러] 두루누비 코스 동기화 작업을 성공적으로 완료했습니다.");
        } catch (Exception e) {
            log.error("[스케줄러] 두루누비 코스 동기화 작업 중 오류가 발생했습니다.", e);
        }
    }

}
