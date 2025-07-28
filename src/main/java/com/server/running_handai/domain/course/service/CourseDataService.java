package com.server.running_handai.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.server.running_handai.domain.course.client.DurunubiApiClient;
import com.server.running_handai.domain.course.dto.*;
import com.server.running_handai.domain.course.dto.DurunubiApiResponseDto.Item;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseImage;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.RoadCondition;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.RoadConditionRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.domain.course.util.TrackPointSimplificationUtil;
import com.server.running_handai.global.response.exception.BusinessException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.*;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import static com.server.running_handai.global.response.ResponseCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDataService {

    public static final String WHITE_SPACE = " ";
    public static final String TARGET_REGION = "부산";
    public static final int RUNNING_SPEED = 9;

    private final GeometryFactory geometryFactory;
    private final RestTemplate restTemplate;
    private final XmlMapper xmlMapper;
    private final ObjectMapper objectMapper;

    private final DurunubiApiClient durunubiApiClient;
    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final RoadConditionRepository roadConditionRepository;
    private final KakaoMapService kakaoMapService;
    private final OpenAiService openAiService;
    private final FileService fileService;

    @Value("classpath:prompt/save-road-condition.st")
    private Resource getRoadConditionPrompt;

    @Value("classpath:prompt/save-level-and-road-condition.st")
    private Resource getLevelAndRoadConditionPrompt;

    @Value("${spring.ai.openai.input-max-tokens}")
    private int inputMaxToken;

    @Value("${course.simplification.distance-tolerance}")
    private double distanceTolerance;

    /**
     * 두루누비 API 관련
     */
    @Async("syncCourseTaskExecutor")
    @Transactional
    public void synchronizeCourseData() {
        log.info("[두루누비 코스 동기화] 작업을 시작합니다.");

        // 두루누비 API로부터 받은 코스 데이터로 apiCourseMap 생성 (key: externalId)
        Map<String, Item> apiCourseMap = fetchAllCoursesFromApi();
        if (apiCourseMap.isEmpty()) {
            log.warn("[두루누비 코스 동기화] API로부터 가져온 데이터가 없습니다. 동기화를 중단합니다.");
            return;
        }

        // DB에 저장된 두루누비 코스 데이터로 dbCourseMap 생성 (key: externalId)
        Map<String, Course> dbCourseMap = courseRepository.findByExternalIdIsNotNull().stream()
                .collect(Collectors.toMap(Course::getExternalId, course -> course));

        // API 데이터를 기준으로 루프를 돌며 DB 데이터와 비교
        List<Course> toSave = new ArrayList<>();
        for (Map.Entry<String, DurunubiApiResponseDto.Item> entry : apiCourseMap.entrySet()) {
            String externalId = entry.getKey();
            Item courseItem = entry.getValue();

            // trackpoints 생성
            List<TrackPoint> trackPoints = parseTrackPoints(courseItem.getGpxPath());
            if (trackPoints.isEmpty()) {
                log.warn("[두루누비 코스 동기화] 코스의 트랙포인트가 없습니다. 해당 코스를 건너뜁니다. externalId: {}", externalId);
                continue;
            }

            // course 생성
            Course apiCourse = parseCourse(courseItem, trackPoints);
            if (apiCourse == null) {
                log.warn("[두루누비 코스 동기화] 코스 파싱 중 오류가 발생했습니다. 해당 코스를 건너뜁니다. externalId: {}", externalId);
                continue;
            }

            Course dbCourse = dbCourseMap.get(externalId); // 현재 DB에 저장된 Course
            if (dbCourse != null) { // DB에 이미 존재 -> 업데이트
                trackPointRepository.deleteByCourseId(dbCourse.getId());
                trackPoints.forEach(trackPoint -> trackPoint.setCourse(dbCourse));
                trackPointRepository.saveAll(trackPoints);
                dbCourse.setStartPoint(apiCourse.getStartPoint());
                dbCourse.updateElevation(apiCourse.getMinElevation(), apiCourse.getMaxElevation());
                log.info("[두루누비 코스 동기화] 트랙포인트 업데이트 완료: courseId={}, count={}", dbCourse.getId(), trackPoints.size());

                if (dbCourse.syncWith(apiCourse)) {
                    toSave.add(dbCourse);
                    log.info("[두루누비 코스 동기화] 코스 데이터 변경 감지 (UPDATE): courseId={}, externalId={}", dbCourse.getId(), externalId);
                }
                dbCourseMap.remove(externalId); // 업데이트 끝난 DB 데이터는 맵에서 제거 (남은 데이터는 DELETE 대상)
            } else { // DB에 없음 -> 신규 추가
                trackPoints.forEach(trackPoint -> trackPoint.setCourse(apiCourse));
                toSave.add(apiCourse);
                log.info("[두루누비 코스 동기화] 신규 코스 저장 (INSERT): externalId={}", externalId);
            }
        }

        // 추가 또는 수정된 Course 저장
        if (!toSave.isEmpty()) {
            courseRepository.saveAll(toSave);
            log.info("[두루누비 코스 동기화] {}건의 코스 데이터가 추가/수정되었습니다.", toSave.size());
        }

        // DB에만 있고 두루누비에서 없어진 Course 삭제
        if (!dbCourseMap.isEmpty()) {
            List<Course> toDelete = new ArrayList<>(dbCourseMap.values());
            courseRepository.deleteAll(toDelete);
            log.info("[두루누비 코스 동기화] {}건의 오래된 코스 삭제(DELETE)", toDelete.size());
            toDelete.forEach(course -> log.debug("[두루누비 코스 동기화] 삭제된 코스: courseId={}, externalId={}", course.getId(), course.getExternalId()));
        }

        log.info("[두루누비 코스 동기화] 작업을 완료했습니다.");
    }

    private Map<String, DurunubiApiResponseDto.Item> fetchAllCoursesFromApi() {
        Map<String, DurunubiApiResponseDto.Item> allItems = new HashMap<>();

        int pageNo = 1;
        final int numOfRows = 50;
        int totalCount = -1;

        do {
            DurunubiApiResponseDto responseDto = durunubiApiClient.fetchCourseData(pageNo, numOfRows);

            if (responseDto == null || responseDto.getResponse().getBody() == null || responseDto.getResponse().getBody().getItems() == null) {
                log.warn("[두루누비 코스 동기화] {} 페이지에서 데이터를 가져오지 못했습니다.", pageNo);
                break;
            }

            if (totalCount == -1) {
                totalCount = responseDto.getResponse().getBody().getTotalCount();
            }

            List<DurunubiApiResponseDto.Item> items = responseDto.getResponse().getBody().getItems().getItemList();
            if (items == null || items.isEmpty()) {
                log.warn("[두루누비 코스 동기화] {} 페이지의 item[] 데이터가 없습니다.", pageNo);
                break;
            }

            for (DurunubiApiResponseDto.Item item : items) {
                if (!item.getSigun().startsWith(TARGET_REGION)) continue;
                allItems.put(item.getCourseIndex(), item);
            }

            pageNo++;
        } while ((pageNo - 1) * numOfRows < totalCount);

        return allItems;
    }

    private Course parseCourse(DurunubiApiResponseDto.Item item, List<TrackPoint> trackPoints) {
        if (item == null) {
            log.warn("[두루누비 코스 동기화] API 응답의 Item 필드가 null입니다. 해당 코스를 건너뜁니다.");
            return null;
        }

        try {
            String externalId = item.getCourseIndex();
            if (isFieldInvalid(externalId, "courseIndex(externalId)", null)) {
                return null;
            }

            String name = item.getCourseName();
            if (isFieldInvalid(name, "courseName", externalId)) {
                return null;
            }

            String distanceValue = item.getCourseDistance();
            if (isIntegerInvalid(distanceValue, "courseDistance", externalId)) {
                return null;
            }
            int distance = Integer.parseInt(distanceValue);

            String durationValue = item.getTotalRequiredTime();
            if (isIntegerInvalid(durationValue, "totalRequiredTime", externalId)) {
                return null;
            }
            double durationInMinutes = (double) distance / RUNNING_SPEED * 60.0;
            int duration = (int) Math.round(durationInMinutes);

            String levelValue = item.getCourseLevel();
            if (isFieldInvalid(levelValue, "courseLevel", externalId)) {
                return null;
            }
            CourseLevel level = CourseLevel.fromApiValue(item.getCourseLevel());

            String tourPoint = item.getTourInfo();

            String gpxPath = item.getGpxPath();
            if (isFieldInvalid(gpxPath, "gpxPath", externalId)) {
                return null;
            }

            Point startPoint = extractStartPoint(trackPoints);
            double minElevation = calculateMinElevation(trackPoints);
            double maxElevation = calculateMaxElevation(trackPoints);

            String sigun = item.getSigun();
            if (isFieldInvalid(sigun, "sigun", externalId)) {
                return null;
            }

            String districtName = sigun.split(WHITE_SPACE)[1]; // 구 단위 행정구역명
            Area area;
            if (districtName.equals("해운대구")) { // 해운대구인 경우, 카카오 지도 API 사용하여 동 단위 분류
                JsonNode startAddress = kakaoMapService.getAddressFromCoordinate(startPoint.getX(), startPoint.getY());
                area = extractArea(startAddress);
            } else {
                area = Area.findBySubRegion(districtName).orElseThrow(() -> {
                    log.error("[두루누비 코스 동기화] 지역 파싱을 실패했습니다. subRegionName: {}", districtName);
                    return new BusinessException(AREA_NOT_FOUND);
                });
            }

            Course course = Course.builder()
                    .externalId(externalId)
                    .name(name)
                    .distance(distance)
                    .duration(duration)
                    .level(level)
                    .tourPoint(tourPoint)
                    .area(area)
                    .gpxPath(gpxPath)
                    .startPoint(startPoint)
                    .minElevation(minElevation)
                    .maxElevation(maxElevation)
                    .build();

            Theme.findBySubRegion(districtName).forEach(course::addTheme);
            return course;
        } catch (Exception e) {
            log.error("[두루누비 코스 동기화] API 데이터 파싱 중 예상치 못한 예외가 발생했습니다. courseIndex: {}", item.getCourseIndex(), e);
            return null;
        }
    }

    /**
     * 필드가 null이거나 비어있는지 검사하고, 유효하지 않은 경우 로그를 남깁니다.
     *
     * @param value       검사할 필드의 값
     * @param fieldName   로그에 표시될 필드의 이름
     * @param courseIndex externalId 값
     * @return 필드가 유효하지 않으면 true, 유효하면 false
     */
    private boolean isFieldInvalid(String value, String fieldName, String courseIndex) {
        if (value == null || value.isBlank()) {
            if (courseIndex == null) {
                log.warn("API 데이터에 '{}' 필드가 없습니다. 해당 코스를 건너뜁니다.", fieldName);
            } else {
                log.warn("[CourseIndex: {}] API 데이터에 '{}' 필드가 없습니다. 해당 코스를 건너뜁니다.", courseIndex, fieldName);
            }
            return true;
        }
        return false;
    }

    /**
     * null, 공백, 숫자 형식 오류를 모두 검사하고, 유효하지 않은 경우 로그를 남깁니다.
     *
     * @return 필드가 유효하지 않으면 false, 유효하지 않으면 true
     */
    private boolean isIntegerInvalid(String value, String fieldName, String courseIndex) {
        if (isFieldInvalid(value, fieldName, courseIndex)) {
            return true;
        }
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[CourseIndex: {}] 필드 '{}'의 값이 올바른 숫자 형식이 아닙니다. (value: {})", courseIndex, fieldName, value);
            return true;
        }
        return false;
    }

    /**
     * GPX 파일을 다운로드 및 파싱하여, 해당 GPX 파일의 모든 좌표 정보를 TrackPoint 엔티티로 만듭니다.
     */
    private List<TrackPoint> parseTrackPoints(String gpxPath) {
        List<TrackPoint> trackPoints = new ArrayList<>();
        try {
            long startTime = System.currentTimeMillis();

            // GPX 파일 다운로드
            String gpxContent = restTemplate.getForObject(gpxPath, String.class);
            if (gpxContent == null || gpxContent.isEmpty()) {
                log.warn("[두루누비 코스 동기화] GPX 파일이 비어있습니다. gpxPath: {}", gpxPath);
                return List.of();
            }

            // Jackson을 사용한 XML 파싱
            GpxDto gpx = xmlMapper.readValue(gpxContent, GpxDto.class);
            List<GpxDto.Trkpt> allPoints = gpx.getTrk().getTrksegs().stream()
                    .flatMap(segment -> segment.getTrkpts().stream())
                    .toList();

            if (allPoints.isEmpty()) {
                log.warn("[두루누비 코스 동기화] GPX 데이터에 트랙포인트가 없습니다.");
                return List.of();
            }

            // TrackPoint 엔티티 생성
            AtomicInteger sequence = new AtomicInteger(1);
            for (GpxDto.Trkpt point : allPoints) {
                TrackPoint trackPoint = TrackPoint.builder()
                        .lat(point.getLat())
                        .lon(point.getLon())
                        .ele(point.getEle())
                        .sequence(sequence.getAndIncrement())
                        .build();
                trackPoints.add(trackPoint);
            }
            long endTime = System.currentTimeMillis();
            log.info("[두루누비 코스 동기화] {}개의 트랙포인트 생성 완료 (소요 시간: {}ms)", trackPoints.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("[두루누비 코스 동기화] 트랙포인트 파싱 중 오류 발생", e);
        }
        return trackPoints;
    }

    /**
     * 코스의 길 상태(road_condition) 정보를 OpenAI API 호출을 통해 저장합니다.
     * OpenAI API의 경우, 예상 토큰 값을 계산하여 최대 토큰 값을 넘으면 RDP 단순화 알고리즘을 적용하여 요청합니다.
     * 기존 데이터가 있는 경우, 일괄 삭제 후 새로 생성된 설명을 저장됩니다.
     * 하나의 코스 데이터 당 5개의 길 상태 정보가 생성됩니다.
     *
     * @param courseId 길 상태를 수정할 course Id
     */
    @Transactional
    public void updateRoadConditions(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
        List<SequenceTrackPointDto> trackPointDtoList = trackPointRepository.findByCourseIdOrderBySequenceAsc(courseId)
                .stream()
                .map(SequenceTrackPointDto::sequenceTrackPointDto)
                .toList();

        // 프롬프트 변수 준비
        Map<String,Object> variables = new HashMap<>();
        variables.put("name", course.getName());
        variables.put("distance", course.getDistance());
        variables.put("duration", course.getDuration());
        variables.put("level", course.getLevel().toString());
        variables.put("trackPoints", convertTrackPointToJson(trackPointDtoList));

        // 요청 프롬프트 토큰 수 확인 후 초과할 경우 RDP 알고리즘 적용
        int requestToken = openAiService.calculateRequestToken(getRoadConditionPrompt, variables);
        if (requestToken > inputMaxToken) {
            log.info("[길 상태 수정] 토큰 초과, RDP 적용 시작: courseId={} requestToken={}", courseId, requestToken);
            variables = simplifyTrackPointsWhileTokenLimit(getRoadConditionPrompt, trackPointDtoList, variables);
        } else {
            log.info("[길 상태 수정] 토큰 초과하지 않아 원본 데이터 사용: courseId={} requestToken={}", courseId, requestToken);
        }

        // OpenAI API 호출 후 파싱 (응답은 "|"로 구분된 5개 설명으로 이루어짐)
        String openAiResponse = openAiService.getOpenAiResponse(getRoadConditionPrompt, variables);
        log.info("[길 상태 수정] OpenAI 응답 수신: response={}", openAiResponse);
        List<String> descriptions = parseResponse(openAiResponse, 5);

        // Open API 응답 유효성 검증
        if (descriptions.isEmpty()) {
            log.error("[길 상태 수정] OpenAI로부터 유효한 응답을 받지 못했습니다. courseId={}", courseId);
            throw new BusinessException(OPENAI_RESPONSE_INVALID);
        }

        // 기존 데이터 일괄 삭제 후 새로 저장
        roadConditionRepository.deleteByCourseId(courseId);
        log.info("[길 상태 수정] 기존 길 상태 데이터 삭제 완료: courseId={}", courseId);

        List<RoadCondition> newRoadConditions = descriptions.stream()
                .map(description -> new RoadCondition(course, description))
                .toList();

        roadConditionRepository.saveAll(newRoadConditions);
        log.info("[길 상태 수정] DB에 길 상태 정보 갱신 완료: courseId={}", courseId);
    }

    /**
     * 저장된 코스의 썸네일 이미지를 S3 버킷에 저장 후, DB에 저장합니다.
     * Course와 CourseImage는 1:1 관계이므로 이미 저장된 이미지가 있을 경우, 이전 이미지는 S3 버킷 내에서 삭제합니다.
     * S3 버킷의 디렉토리는 "image"로 지정합니다.
     *
     * @param courseImageFile 업로드된 이미지 파일
     */
    @Transactional
    public void updateCourseImage(Long courseId, MultipartFile courseImageFile) throws IOException {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 새 파일을 S3에 먼저 업로드
        String newImageUrl = fileService.uploadFile(courseImageFile, "image");
        log.info("[코스 이미지 수정] S3 버킷에 이미지 업로드 완료: newImageUrl={}", newImageUrl);

        // 삭제할 기존 파일 URL을 임시 변수에 저장
        String oldImageUrl = (course.getCourseImage() != null) ? course.getCourseImage().getImgUrl() : null;

        // DB 정보 업데이트
        if (oldImageUrl != null) {
            course.getCourseImage().updateImageUrl(newImageUrl);
        } else {
            course.updateCourseImage(new CourseImage(newImageUrl));
        }
        log.info("[코스 이미지 수정] DB에 이미지 정보 갱신 완료: Course ID={}", courseId);

        // 트랜잭션이 성공적으로 커밋된 후에 기존 파일 삭제
        if (oldImageUrl != null) {
            fileService.deleteFile(oldImageUrl);
            log.info("[코스 이미지 수정] S3에서 기존 이미지 삭제: Course Id={}, URL={}", course.getCourseImage().getCourseImageId(), course.getCourseImage().getImgUrl());
        }
    }

    /**
     * GPX 파일을 받아 코스 정보를 생성하고 저장합니다.
     * OpenAI API의 경우, 예상 토큰 값을 계산하여 최대 토큰 값을 넘으면 RDP 단순화 알고리즘을 적용하여 요청합니다.
     * S3 버킷의 디렉토리는 "gpx"로 지정합니다.
     *
     * @param gpxCourseRequestDto 코스 출발지, 도착지 존재
     * @param courseGpxFile 업로드된 GPX 파일
     */
    @Transactional
    public void createCourseToGpx(GpxCourseRequestDto gpxCourseRequestDto, MultipartFile courseGpxFile) throws IOException {
        log.info("[GPX 코스 생성] 시작: 파일명={}, 크기={} bytes", courseGpxFile.getOriginalFilename(), courseGpxFile.getSize());

        // 1. 코스 이름 조합
        // todo: 코스명 이름이 중복되는 경우 추가적인 처리 필요
        String courseName = gpxCourseRequestDto.startPointName() + "-" + gpxCourseRequestDto.endPointName();

        // 2. GPX 파일의 track point 파싱
        List<TrackPoint> trackPoints;
        try {
            trackPoints = getTrackPoints(courseGpxFile);
            log.info("[GPX 코스 생성] 트랙포인트 파싱 완료 ({}개)", trackPoints.size());
        } catch (Exception e) {
            log.error("[GPX 코스 생성] 트랙포인트 파싱 실패", e);
            throw new BusinessException(GPX_FILE_PARSE_FAILED);
        }

        // 3. 전체 거리 계산
        int distance = calculateDistance(trackPoints);
        log.info("[GPX 코스 생성] 전체 거리 계산 완료: {}km", distance);

        // 4. 소요 시간 계산 (9km/h 속도 기준)
        int duration = calculateDuration(distance, 9.0);
        log.info("[GPX 코스 생성] 소요 시간 계산 완료: {}분", duration);

        // 5. 최대, 최소 고도 계산
        double maxElevation = calculateMaxElevation(trackPoints);
        double minElevation = calculateMinElevation(trackPoints);
        log.info("[GPX 코스 생성] 고도 계산 완료 (최대: {}, 최소: {})", maxElevation, minElevation);

        // 6. 시작점을 기준으로 Area 분류 (카카오 지도 API 호출)
        Point startPoint = extractStartPoint(trackPoints);
        JsonNode startAddress = kakaoMapService.getAddressFromCoordinate(startPoint.getX(), startPoint.getY());
        Area area = extractArea(startAddress);
        log.info("[GPX 코스 생성] Area 분류 완료: {}", area);

        // 7. level, road condition을 위한 OpenAI API 호출 (응답은 "|"로 구분된 6개 설명으로 이루어짐)
        List<SequenceTrackPointDto> trackPointDtoList = trackPoints.stream()
                .map(SequenceTrackPointDto::sequenceTrackPointDto)
                .toList();

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", courseName);
        variables.put("distance", distance);
        variables.put("duration", duration);
        variables.put("trackPoints", convertTrackPointToJson(trackPointDtoList));

        // 요청 프롬프트 토큰 수 확인 후 초과할 경우 RDP 알고리즘 적용
        int requestToken = openAiService.calculateRequestToken(getLevelAndRoadConditionPrompt, variables);
        if (requestToken > inputMaxToken) {
            log.info("[GPX 코스 생성] 토큰 초과, RDP 적용 시작: requestToken={}", requestToken);
            variables = simplifyTrackPointsWhileTokenLimit(getLevelAndRoadConditionPrompt, trackPointDtoList, variables);
        } else {
            log.info("[GPX 코스 생성] 토큰 초과하지 않아 원본 데이터 사용: requestToken={}", requestToken);
        }

        // OpenAI API 호출
        String openAiResponse = openAiService.getOpenAiResponse(getLevelAndRoadConditionPrompt, variables);
        log.info("[GPX 코스 생성] OpenAI 응답 수신: {}", openAiResponse);
        List<String> descriptions = parseResponse(openAiResponse, 6);

        String responseLevel = descriptions.getFirst();
        CourseLevel level;

        // "EASY", "MEDIUM", "HARD" 중 하나면 그대로, 아니면 MEDIUM (기본값)
        if ("EASY".equals(responseLevel) || "MEDIUM".equals(responseLevel) || "HARD".equals(responseLevel)) {
            level = CourseLevel.valueOf(responseLevel);
        } else {
            log.warn("[GPX 코스 생성] OpenAI 난이도 응답값이 예상과 다름: {}, 기본값 MEDIUM으로 대체", responseLevel);
            level = CourseLevel.MEDIUM;
        }

        // 8. AWS S3에 GPX 파일 업로드
        String gpxPath = fileService.uploadFile(courseGpxFile, "gpx");

        // 9. course, road condition, track point DB에 저장
        Course course = Course.builder()
                .name(courseName)
                .distance(distance)
                .duration(duration)
                .level(level)
                .area(area)
                .gpxPath(gpxPath)
                .startPoint(startPoint)
                .maxElevation(maxElevation)
                .minElevation(minElevation)
                .build();

        // 10. Theme 설정
        String districtName = startAddress.path("address").path("region_2depth_name").asText(); // TODO 구 단위 변수명 수정
        Theme.findBySubRegion(districtName).forEach(course::addTheme);

        courseRepository.save(course);
        log.info("[GPX 코스 생성] Course 저장 완료: ID={}", course.getId());

        List<RoadCondition> roadConditions = descriptions.stream()
                .skip(1)
                .limit(5)
                .map(description -> new RoadCondition(course, description))
                .toList();

        roadConditionRepository.saveAll(roadConditions);
        log.info("[GPX 코스 생성] RoadCondition {}개 저장 완료", roadConditions.size());

        for (TrackPoint trackPoint : trackPoints) {
            trackPoint.setCourse(course);
        }
        trackPointRepository.saveAll(trackPoints);
        log.info("[GPX 코스 생성] TrackPoint {}개 저장 완료", trackPoints.size());

        log.info("[GPX 코스 생성] 전체 작업 완료: 코스명={})", courseName);
    }

    /**
     * GPX 파일에서 트랙포인트 목록을 추출합니다.
     * 추출할 때, 트랙(trk)이 있으면 우선적으로 파싱에 사용하고, 없으면 루트(rte)를 사용합니다.
     *
     * @param courseGpxFile 업로드된 GPX 파일
     * @return 추출된 TrackPoint 리스트 (lat, lon, ele, sequence)
     */
    private List<TrackPoint> getTrackPoints(MultipartFile courseGpxFile) throws Exception {
        // DTO에 정의되지 않은 필드 무시
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, true);

        GpxDto gpx = xmlMapper.readValue(courseGpxFile.getInputStream(), GpxDto.class);

        List<GpxDto.Trkpt> trkPoints = new ArrayList<>();
        List<GpxDto.Rtept> rtePoints = new ArrayList<>();

        // 트랙(trk) 구조 파싱
        if (gpx.getTrk() != null && gpx.getTrk().getTrksegs() != null) {
            trkPoints = gpx.getTrk().getTrksegs().stream()
                    .filter(trkseg -> trkseg.getTrkpts() != null)
                    .flatMap(trkseg -> trkseg.getTrkpts().stream())
                    .toList();
        }

        // 루트(rte) 구조 파싱
        if (gpx.getRte() != null && gpx.getRte().getRtepts() != null) {
            rtePoints = gpx.getRte().getRtepts();
        }

        // 트랙포인트 변환 (트랙 우선, 없으면 루트 사용)
        List<TrackPoint> trackPoints = new ArrayList<>();
        AtomicInteger sequence = new AtomicInteger(1);

        if (!trkPoints.isEmpty()) {
            trackPoints = trkPoints.stream()
                    .map(pt -> TrackPoint.builder()
                            .lat(pt.getLat())
                            .lon(pt.getLon())
                            .ele(pt.getEle())
                            .sequence(sequence.getAndIncrement())
                            .build())
                    .collect(Collectors.toList());
        } else if (!rtePoints.isEmpty()) {
            trackPoints = rtePoints.stream()
                    .map(pt -> TrackPoint.builder()
                            .lat(pt.getLat())
                            .lon(pt.getLon())
                            .ele(pt.getEle())
                            .sequence(sequence.getAndIncrement())
                            .build())
                    .collect(Collectors.toList());
        }
        return trackPoints;
    }

    /**
     * 트랙포인트 리스트로부터 코스 전체 거리(distance)를 계산합니다.
     *
     * @param trackPoints 트랙포인트 리스트
     * @return 전체 거리 (km, 소수점 반올림)
     */
    private int calculateDistance(List<TrackPoint> trackPoints) {
        double totalDistance = 0.0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint previous = trackPoints.get(i - 1);
            TrackPoint current = trackPoints.get(i);
            totalDistance += haversine(previous.getLat(), previous.getLon(), current.getLat(), current.getLon());
        }
        return (int) Math.round(totalDistance);
    }

    /**
     * 두 좌표 간 거리를 하버사인 공식으로 계산합니다.
     * 하버사인 공식은 지구 위의 두 점 사이의 최단 거리를 구할 때 사용합니다.
     *
     * @param lat1 첫 번째 위도
     * @param lon1 첫 번째 경도
     * @param lat2 두 번째 위도
     * @param lon2 두 번째 경도
     * @return 두 지점 간 거리 (km)
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 코스 전체 거리와 속도로 소요 시간(duration)을 계산합니다.
     *
     * @param distance 거리 (km)
     * @param speed 속도 (km/h)
     * @return 소요 시간 (분)
     */
    private int calculateDuration(double distance, double speed) {
        double hours = distance / speed;
        return (int) Math.round(hours * 60);
    }

    /**
     * 트랙포인트 리스트에서 최대 고도를 계산합니다.
     *
     * @param trackPoints 트랙포인트 리스트
     * @return 최대 고도 (m)
     */
    private double calculateMaxElevation(List<TrackPoint> trackPoints) {
        return trackPoints.stream().mapToDouble(TrackPoint::getEle).max().orElse(0.0);
    }

    /**
     * 트랙포인트 리스트에서 최소 고도를 계산합니다.
     *
     * @param trackPoints 트랙포인트 리스트
     * @return 최소 고도 (m)
     */
    private double calculateMinElevation(List<TrackPoint> trackPoints) {
        return trackPoints.stream().mapToDouble(TrackPoint::getEle).min().orElse(0.0);
    }

    /**
     * 트랙포인트 리스트에서 시작점 좌표(startPoint)를 추출합니다.
     *
     * @param trackPoints 트랙포인트 리스트
     * @return 시작점 좌표 (Point), 없으면 null
     */
    private Point extractStartPoint(List<TrackPoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) return null;
        TrackPoint first = trackPoints.getFirst();
        return geometryFactory.createPoint(new Coordinate(first.getLon(), first.getLat()));
    }

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 행정구역(Area)을 추출합니다.
     * 도로명 주소(road_address)는 좌표에 따라 반환되지 않을 수 있기 때문에 지번 주소(address)를 기준으로 합니다.
     *
     * @param jsonNode 주소 정보 JSON
     * @return Area, 없으면 Area.UNKNOWN
     */
    private Area extractArea(JsonNode jsonNode) {
        String districtName = jsonNode.path("address").path("region_2depth_name").asText(); // 구 단위
        String dongName = jsonNode.path("address").path("region_3depth_name").asText(); // 동 단위

        // Area 설정
        Area area;
        if (dongName.equals("송정동")) {
            area = Area.SONGJEONG_GIJANG;
        } else {
            area = Area.findBySubRegion(districtName).orElseGet(() -> {
                log.warn("[GPX 코스 생성] 행정구역 매칭 실패: districtName='{}', dongName='{}'. Area.UNKNOWN 반환", districtName, dongName);
                return Area.UNKNOWN;
            });
        }
        return area;
    }

    /**
     * "|" 기준으로 파싱하여 리스트로 반환합니다.
     *
     * @param response OpenAI 응답 문자열
     * @param parseNumber 최대 파싱 개수
     * @return 파싱된 리스트
     */
    private List<String> parseResponse(String response, int parseNumber) {
        if (response == null || response.isBlank()) {
            log.warn("[OpenAI 응답 파싱] 응답이 null 또는 빈 문자열");
            return List.of();
        }

        String[] rawData = response.split("\\|");
        List<String> parseData = new ArrayList<>();

        for (int i = 0; i < Math.min(parseNumber, rawData.length); i++) {
            // 응답이 잘못되어 "난이도: {설명}" 형태로 출력될 경우를 대비
            String data = rawData[i].trim();
            if (data.contains(":")) {
                data = data.substring(data.indexOf(":") + 1).trim();
            }
            parseData.add(data);
        }

        if (parseData.size() < parseNumber) {
            log.warn("[OpenAI 응답 파싱] 파싱 개수 부족: 기대치={}, 실제={}, 원본 응답={}", parseNumber, parseData.size(), response);
        }

        return parseData;
    }

    /**
     * 토큰을 초과하지 않을 때까지 RDP 알고리즘을 적용합니다.
     *
     * @param promptTemplate 프롬프트 템플릿
     * @param trackPointDtoList 트랙포인트 Dto
     * @param variables 프롬프트 변수
     * @return newVariables 새로운 프롬프트 변수
     */
    private Map<String, Object> simplifyTrackPointsWhileTokenLimit(Resource promptTemplate, List<SequenceTrackPointDto> trackPointDtoList, Map<String, Object> variables) {
        double tolerance = distanceTolerance;
        List<SequenceTrackPointDto> newTrackPointDtoList = trackPointDtoList;

        while (true) {
            newTrackPointDtoList = TrackPointSimplificationUtil.simplifyTrackPoints(newTrackPointDtoList, tolerance, geometryFactory);

            // 단순화된 트랙 포인트로 프롬프트 변수 업데이트
            Map<String, Object> newVariables = new HashMap<>(variables);
            newVariables.put("trackPoints", convertTrackPointToJson(newTrackPointDtoList));

            int requestToken = openAiService.calculateRequestToken(promptTemplate, newVariables);
            if (requestToken <= inputMaxToken) {
                log.info("[RDP 알고리즘 적용] 적용 완료: tolearance={} trackPoint={} requestToken={}", tolerance, newTrackPointDtoList.size(), requestToken);
                return newVariables;
            }

            // 토큰이 여전히 초과할 경우, tolerance를 증가시켜 더 단순화
            tolerance *= 2;
        }
    }

    /**
     * OpenAI API 요청 시 모델 인식률을 높이기 위해 JSON 문자열로 변환합니다.
     *
     * @param trackPointDtoList 트랙 포인트 Dto
     * @return JSON
     */
    private String convertTrackPointToJson(List<SequenceTrackPointDto> trackPointDtoList) {
        try {
            return objectMapper.writeValueAsString(trackPointDtoList).replace("\"", "\\\"");
        } catch (JsonProcessingException e){
            log.warn("[트랙포인트 JSON 변환] 실패: message={}", e.getMessage());
            return trackPointDtoList.toString();
        }
    }
}
