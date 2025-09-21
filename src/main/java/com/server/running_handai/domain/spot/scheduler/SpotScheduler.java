package com.server.running_handai.domain.spot.scheduler;

import com.server.running_handai.domain.spot.service.SpotDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotScheduler {
    private final SpotDataService spotDataService;

//    /**
//     * 매주 월요일 새벽 4시에 즐길거리 위치 정보 동기화 작업을 실행합니다.
//     * cron = "[초] [분] [시] [일] [월] [요일]"
//     */
//    @Scheduled(cron = "0 0 4 * * 1", zone = "Asia/Seoul")
//    public void scheduleSyncSpotsByLocation() {
//        log.info("[스케줄러] 즐길거리 위치 정보 동기화 작업을 시작합니다.");
//        try {
//            spotDataService.syncSpotsByLocation();
//            log.info("[스케줄러] 즐길거리 위치 정보 동기화 작업을 성공적으로 완료했습니다.");
//        } catch (Exception e) {
//            log.error("[스케줄러] 즐길거리 위치 정보 동기화 작업 중 오류가 발생했습니다.", e);
//        }
//    }

    /**
     * 매일 새벽 5시 30분에 즐길거리 장소 정보 동기화 작업을 실행합니다.
     * cron = "[초] [분] [시] [일] [월] [요일]"
     */
    @Scheduled(cron = "0 30 5 * * *", zone = "Asia/Seoul")
    public void scheduleSyncSpotsByDate() {
        log.info("[스케줄러] 즐길거리 장소 정보 동기화 작업을 시작합니다.");
        try {
            // 호출 기준 전날 날짜 계산 (YYYYMMDD)
            String date = LocalDate.now()
                    .minusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            spotDataService.syncSpotsByDate(date);
            log.info("[스케줄러] 즐길거리 장소 정보 동기화 작업을 성공적으로 완료했습니다.");
        } catch (Exception e) {
            log.error("[스케줄러] 즐길거리 장소 정보 동기화 작업 중 오류가 발생했습니다.", e);
        }
    }
}
