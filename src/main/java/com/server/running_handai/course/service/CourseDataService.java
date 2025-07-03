package com.server.running_handai.course.service;

import static com.server.running_handai.global.exception.ErrorCode.AREA_NOT_FOUND;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.server.running_handai.course.client.DurunubiApiClient;
import com.server.running_handai.course.dto.DurunubiApiResponseDto;
import com.server.running_handai.course.dto.DurunubiApiResponseDto.Item;
import com.server.running_handai.course.dto.GpxDto;
import com.server.running_handai.course.dto.RoadConditionResponseDto;
import com.server.running_handai.course.entity.*;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.course.repository.RoadConditionRepository;
import com.server.running_handai.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.server.running_handai.global.exception.ErrorCode;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDataService {

    public static final int DEFAULT_DISTANCE = 0;
    public static final int DEFAULT_TIME = 0;
    public static final String WHITE_SPACE = " ";

    private final DurunubiApiClient durunubiApiClient;
    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();

    private final ChatClient.Builder chatClientBuilder;
    private final RoadConditionRepository roadConditionRepository;

    @Value("classpath:prompt/save-road-condition.st")
    private Resource getRoadConditionPrompt;

    /** 두루누비 API 관련 */
    @Async("syncCourseTaskExecutor")
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
            log.warn("API 데이터에 courseIndex(externalId)가 없습니다. 해당 코스를 건너뜁니다. item: {}", item);
            return null;
        }
        if (name == null || name.isBlank()) {
            log.warn("[CourseIndex: {}] API 데이터에 courseName이 없습니다. 해당 코스를 건너뜁니다.", externalId);
            return null;
        }
        if (gpxPath == null || gpxPath.isBlank()) {
            log.warn("[CourseIndex: {}] GPX 경로가 없습니다. 해당 코스를 건너뜁니다.", externalId);
            return null;
        }

        try {
            // 숫자, 문자열 등 각 필드를 안전하게 파싱하고 기본값 할당
            int distance = safeParseInt(item.getCourseDistance(), DEFAULT_DISTANCE);
            int duration = safeParseInt(item.getTotalRequiredTime(), DEFAULT_TIME);
            String tourPoint = item.getTourInfo();
            CourseLevel level = CourseLevel.fromApiValue(item.getCourseLevel());

            String subRegionName = item.getSigun().split(WHITE_SPACE)[1];
            Area area = Area.findBySubRegion(subRegionName).orElseThrow(() -> {
                log.error("지역 변환을 실패했습니다. subRegionName: {}", subRegionName);
                return new BusinessException(AREA_NOT_FOUND);
            });

            // TODO synchronizeCourseData()에 시작점, 최대/최소 고도 초기화하는 로직 추가해야함
            GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
            double latitude = 35.08747067199999; // 위도
            double longitude = 129.004480714;    // 경도
            Coordinate coordinate = new Coordinate(longitude, latitude);
            Point point = geometryFactory.createPoint(coordinate);

            return Course.builder()
                    .externalId(externalId)
                    .name(name)
                    .distance(distance)
                    .duration(duration)
                    .level(level)
                    .tourPoint(tourPoint)
                    .area(area)
                    .gpxPath(gpxPath)
                    .startPoint(point)
                    .build();

        } catch (Exception e) {
            log.error("API 데이터 파싱 중 예상치 못한 예외 발생 (courseIndex: {})", externalId, e);
            return null;
        }
    }

    // Null-safe 정수 파싱 헬퍼 메서드
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

    @Async("gpxTaskExecutor")
    @Transactional
    public void syncAllCoursesWithEmptyTrackPoints() {
        List<Course> coursesToSync = courseRepository.findCoursesWithEmptyTrackPoints();

        if (coursesToSync.isEmpty()) {
            log.info("새롭게 트랙포인트를 동기화할 코스가 없습니다.");
            return;
        }

        log.info("총 {}개의 코스에 대해 트랙포인트 동기화를 시작합니다.", coursesToSync.size());
        for (Course course : coursesToSync) {
            saveTrackPoints(course);
            updateCourseStartPoint(course);
        }
    }

    /**
     * Course 객체의 gpxPath에 있는 GPX 파일을 다운로드 및 파싱하여,
     * 해당 GPX 파일의 모든 좌표 정보를 TrackPoint 엔티티로 만들고 데이터베이스에 일괄 저장(Batch Insert)합니다.
     */
    private void saveTrackPoints(Course course) {
        long startTime = System.currentTimeMillis();
        log.info("[Course ID: {}] 트랙포인트 저장 시작.", course.getId());

        try {
            // 1. GPX 파일 다운로드
            String gpxContent = restTemplate.getForObject(course.getGpxPath(), String.class);
            if (gpxContent == null || gpxContent.isEmpty()) {
                log.warn("[Course ID: {}] GPX 파일이 비어있습니다. URL: {}", course.getId(), course.getGpxPath());
                return;
            }

            // 2. Jackson을 사용한 XML 파싱
            GpxDto gpx = xmlMapper.readValue(gpxContent, GpxDto.class);

            // 3. TrackPoint 엔티티 리스트 생성
            List<TrackPoint> trackPointsToSave = new ArrayList<>();
            AtomicInteger sequence = new AtomicInteger(1);

            gpx.getTrk().getTrksegs().forEach(segment ->
                    segment.getTrkpts().forEach(point -> {
                        TrackPoint trackPoint = TrackPoint.builder()
                                .lat(point.getLat())
                                .lon(point.getLon())
                                .ele(point.getEle())
                                .sequence(sequence.getAndIncrement())
                                .build();
                        trackPoint.setCourse(course);
                        trackPointsToSave.add(trackPoint);
                    })
            );

            // 4. Batch Insert 실행
            if (!trackPointsToSave.isEmpty()) {
                trackPointRepository.saveAll(trackPointsToSave);
                long endTime = System.currentTimeMillis();
                log.info("[Course ID: {}] {}개의 트랙포인트 저장 완료. (소요 시간: {}ms)", course.getId(), trackPointsToSave.size(), (endTime - startTime));
            }

        } catch (Exception e) {
            log.error("[Course ID: {}] 트랙포인트 동기화 중 오류 발생", course.getId(), e);
        }
    }

    private void updateCourseStartPoint(Course course) {
        try {
            trackPointRepository.findFirstByCourseOrderBySequenceAsc(course)
                    .ifPresentOrElse(
                            firstTrackPoint -> {
                                Point startPoint = geometryFactory.createPoint(
                                        new Coordinate(firstTrackPoint.getLon(), firstTrackPoint.getLat())
                                );
                                course.setStartPoint(startPoint);
                                log.info("[Course ID: {}] 시작 포인트 저장 완료", course.getId());
                            },
                            () -> log.warn("[Course ID: {}] 시작 포인트를 찾을 수 없습니다.", course.getId())
                    );
        } catch (Exception e) {
            log.error("[Course ID: {}] 시작 포인트 저장 중 오류 발생", course.getId(), e);
        }
    }

    /** OpenAI API 관련 */
    /** 길 상태 수정 */
    @Transactional
    public RoadConditionResponseDto updateRoadCondition(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
        List<TrackPoint> trackPoints = course.getTrackPoints();

        // 1. 프롬프트 변수 준비
        List<Map<String, Object>> trackPointData = trackPoints.stream()
                .map(tp -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("lat", tp.getLat());
                    map.put("lon", tp.getLon());
                    map.put("ele", tp.getEle());
                    map.put("sequence", tp.getSequence());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> variables = Map.of(
                "name", course.getName(),
                "distance", course.getDistance(),
                "duration", course.getDuration(),
                "level", course.getLevel().toString(),
                "trackPoint", trackPointData
        );

        // 2. OpenAI API 호출 (응답은 "|"로 구분된 5개 설명으로 이루어짐)
        String openAiResponse = callOpenAiApi(getRoadConditionPrompt, variables);

        // 3. 데이터 파싱
        List<String> descriptions = parseRoadConditionDescriptions(openAiResponse);

        // 4. 기존 데이터 일괄 삭제 후 새로 저장
        roadConditionRepository.deleteByCourseId(courseId);

        for (String desc : descriptions) {
            RoadCondition rc = new RoadCondition(course, desc);
            roadConditionRepository.save(rc);
        }

        return new RoadConditionResponseDto(course, descriptions);
    }

    /** "|" 기준 응답값 파싱 */
    public List<String> parseRoadConditionDescriptions(String response) {
        String[] rawData = response.split("\\|");
        List<String> parseData = new ArrayList<>();

        for (int i = 0; i < Math.min(5, rawData.length); i++) {
            // 응답이 잘못되어 "난이도: {설명}" 형태로 출력될 경우를 대비
            String data = rawData[i].trim();
            if (data.contains(":")) {
                data = data.substring(data.indexOf(":") + 1).trim();
            }
            parseData.add(data);
        }

        return parseData;
    }

    /** 원하는 프롬프트로 OpenAI API 호출 */
    public String callOpenAiApi(Resource promptResource, Map<String, Object> variables) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            PromptTemplate promptTemplate = new PromptTemplate(promptResource);
            Prompt prompt = promptTemplate.create(variables);

            return chatClient.prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: ", e);
            throw new BusinessException(ErrorCode.OPENAI_API_ERROR);
        }
    }
}
