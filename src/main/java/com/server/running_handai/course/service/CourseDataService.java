package com.server.running_handai.course.service;

import static com.server.running_handai.global.exception.ErrorCode.AREA_NOT_FOUND;

import com.server.running_handai.course.client.DurunubiApiClient;
import com.server.running_handai.course.dto.DurunubiApiResponseDto;
import com.server.running_handai.course.dto.DurunubiApiResponseDto.Item;
import com.server.running_handai.course.entity.Area;
import com.server.running_handai.course.entity.Course;
import com.server.running_handai.course.entity.CourseLevel;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDataService {

    public static final int DEFAULT_DISTANCE = 0;
    public static final int DEFAULT_TIME = 0;

    private final DurunubiApiClient durunubiApiClient;
    private final CourseRepository courseRepository;

    @Async("taskExecutor")
    @Transactional
    public void synchronizeCourseData() {
        log.info("두루누비 Course 데이터 동기화를 시작합니다.");

        // 1. 두루누비 API로부터 받은 코스 데이터로 apiCourseMap 생성 (Key: externalId)
        Map<String, Item> apiCourseMap = fetchAllCoursesFromApi();
        if (apiCourseMap.isEmpty()) {
            log.warn("API로부터 가져온 데이터가 없습니다. 동기화를 중단합니다.");
            return;
        }

        // 2. DB에 저장된 두루누비 코스 데이터로 dbCourseMap 생성 (Key: externalId)
        Map<String, Course> dbCourseMap = courseRepository.findByExternalIdIsNotNull().stream()
                .collect(Collectors.toMap(Course::getExternalId, course -> course));

        List<Course> toSave = new ArrayList<>(); // 추가 또는 수정할 Course 엔티티 목록

        // 3. API 데이터를 기준으로 루프를 돌며 DB 데이터와 비교
        for (Map.Entry<String, DurunubiApiResponseDto.Item> entry : apiCourseMap.entrySet()) {
            String externalId = entry.getKey();
            Course apiCourse = toEntity(entry.getValue()); // 비교 대상이 되는 api로 받은 Course

            if (apiCourse == null) { // toEntity에서 파싱 실패 등으로 null을 반환한 경우
                continue; // 현재 루프를 건너뛰고 다음 코스로 진행
            }

            Course dbCourse = dbCourseMap.get(externalId); // 현재 DB에 저장된 Course
            if (dbCourse != null) {
                // CASE 1: DB에 이미 존재 -> UPDATE 확인
                if (dbCourse.syncWith(apiCourse)) {
                    toSave.add(dbCourse);
                    log.info("코스 변경 감지 (UPDATE): id={}, externalId={}", dbCourse.getId(), externalId);
                }
                // 확인된 DB 데이터는 맵에서 제거 (남은 데이터는 DELETE 대상)
                dbCourseMap.remove(externalId);
            } else {
                // CASE 2: DB에 없음 -> 신규 추가 대상 (INSERT)
                toSave.add(apiCourse);
                log.info("신규 코스 발견 (INSERT): externalId={}", externalId);
            }
        }

        // 4. DB 작업 실행
        if (!toSave.isEmpty()) {
            courseRepository.saveAll(toSave);
            log.info("{}건의 코스 데이터가 추가/수정되었습니다.", toSave.size());
        }

        // 5. DB에만 있고 두루누비에 없는 Course 삭제
        if (!dbCourseMap.isEmpty()) {
            List<Course> toDelete = new ArrayList<>(dbCourseMap.values());
            courseRepository.deleteAll(toDelete);
            log.info("{}건의 오래된 코스 데이터가 삭제되었습니다.", toDelete.size());
            toDelete.forEach(course -> log.debug("삭제된 코스: id={}, externalId={}", course.getId(), course.getExternalId()));
        }

        log.info("Course 데이터 동기화를 완료했습니다.");
    }

    private Map<String, DurunubiApiResponseDto.Item> fetchAllCoursesFromApi() {
        Map<String, DurunubiApiResponseDto.Item> allItems = new HashMap<>();

        int pageNo = 1;
        final int numOfRows = 50;
        int totalCount = -1;

        do {
            DurunubiApiResponseDto responseDto = durunubiApiClient.fetchCourseData(pageNo, numOfRows);
            if (responseDto == null || responseDto.getResponse().getBody() == null || responseDto.getResponse().getBody().getItems() == null) {
                log.warn("{} 페이지에서 데이터를 가져오지 못했습니다.", pageNo);
                break;
            }

            if (totalCount == -1) {
                totalCount = responseDto.getResponse().getBody().getTotalCount();
            }

            List<DurunubiApiResponseDto.Item> items = responseDto.getResponse().getBody().getItems().getItemList();
            if (items == null || items.isEmpty()) {
                log.warn("{} 페이지의 item[] 데이터가 없습니다.", pageNo);
                break;
            }

            for (DurunubiApiResponseDto.Item item : items) {
                if (!item.getSigun().startsWith("부산")) continue;
                allItems.put(item.getCourseIndex(), item);
            }

            pageNo++;
        } while ((pageNo - 1) * numOfRows < totalCount);

        return allItems;
    }

        private Course toEntity(DurunubiApiResponseDto.Item item) {
            if (item == null) {
                log.warn("API 응답의 Item 필드가 null입니다. 건너뜁니다.");
                return null;
            }

            // null이면 안되는 값 검증
            String externalId = item.getCourseIndex();
            String name = item.getCourseName();
            String gpxPath = item.getGpxPath();

            if (externalId == null || externalId.isBlank()) {
                log.warn("API 데이터에 courseIndex(externalId)가 없습니다. 건너뜁니다. item: {}", item);
                return null;
            }
            if (name == null || name.isBlank()) {
                log.warn("API 데이터에 courseName(name)이 없습니다. 건너뜁니다. courseIndex: {}", externalId);
                return null;
            }
            if (gpxPath == null || gpxPath.isBlank()) {
                log.warn("GPX 경로가 없습니다. 코스를 건너뜁니다. courseIndex: {}", externalId);
                return null;
            }

            try {
                // 숫자, 문자열 등 각 필드를 안전하게 파싱하고 기본값 할당
                int distance = safeParseInt(item.getCourseDistance(), DEFAULT_DISTANCE);
                int duration = safeParseInt(item.getTotalRequiredTime(), DEFAULT_TIME);
                String tourPoint = item.getTourInfo();
                String district = safeParseString(item.getSigun());
                CourseLevel level = CourseLevel.fromApiValue(item.getCourseLevel());

                String subRegionName = item.getSigun().split(" ")[1];
                Area area = Area.findBySubRegion(subRegionName).orElseThrow(() -> {
                    log.error("지역 변환을 실패했습니다. subRegionName: {}", subRegionName);
                    return new BusinessException(AREA_NOT_FOUND);
                });

                return Course.builder()
                        .externalId(externalId)
                        .name(name)
                        .distance(distance)
                        .duration(duration)
                        .level(level)
                        .tourPoint(tourPoint)
                        .area(area)
                        .gpxPath(gpxPath)
                        .build();

            } catch (Exception e) {
                // 예상치 못한 다른 모든 예외를 대비
                log.error("API 데이터 파싱 중 예상치 못한 예외 발생. courseIndex: {}", externalId, e);
                return null;
            }
        }

        /**
         * Null-safe 정수 파싱 헬퍼 메서드
         */
        private int safeParseInt(String value, int defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        /**
         * Null-safe 문자열 반환 헬퍼 메서드 (null일 경우 빈 문자열 반환)
         */
        private String safeParseString(String value) {
            return (value != null) ? value : "";
        }
}
