package com.server.running_handai.course.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.server.running_handai.course.client.DurunubiApiClient;
import com.server.running_handai.course.dto.*;
import com.server.running_handai.course.dto.DurunubiApiResponseDto.Item;
import com.server.running_handai.course.entity.*;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.course.repository.RoadConditionRepository;
import com.server.running_handai.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.exception.BusinessException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
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

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();

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
            Course apiCourse = toEntity(entry.getValue());

            if (apiCourse == null) { // toEntity에서 파싱 실패 등으로 null을 반환한 경우
                continue; // 다음 코스로 건너뜀
            }

            Course dbCourse = dbCourseMap.get(externalId); // 현재 DB에 저장된 Course
            if (dbCourse != null) { // DB에 이미 존재 -> 업데이트
                if (dbCourse.syncWith(apiCourse)) { // gpxPath 외의 다른 필드들 동기화
                    toSave.add(dbCourse);
                    log.info("[두루누비 코스 동기화] 코스 데이터 변경 감지 (UPDATE): courseId={}, externalId={}", dbCourse.getId(), externalId);
                }
                if (dbCourse.syncGpxPathWith(apiCourse.getGpxPath())) { // gpx 파일이 달라지면
                    saveTrackPoints(dbCourse); // 트랙포인트 동기화
                    log.info("[두루누비 코스 동기화] gpx 파일 변경 감지 (UPDATE): courseId={}, externalId={}", dbCourse.getId(), externalId);
                }
                dbCourseMap.remove(externalId); // 업데이트 끝난 DB 데이터는 맵에서 제거 (남은 데이터는 DELETE 대상)
            } else { // DB에 없음 -> 신규 추가
                Course savedCourse = courseRepository.save(apiCourse);
                saveTrackPoints(savedCourse);
                log.info("[두루누비 코스 동기화] 신규 코스 발견 (INSERT): externalId={}", externalId);
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
            log.info("[두루누비 코스 동기화] {}건의 오래된 코스 데이터가 삭제되었습니다.", toDelete.size());
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

    private Course toEntity(DurunubiApiResponseDto.Item item) {
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

            String gpxPath = item.getGpxPath();
            if (isFieldInvalid(gpxPath, "gpxPath", externalId)) {
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

            String tourPoint = item.getTourInfo();

            String levelValue = item.getCourseLevel();
            if (isFieldInvalid(levelValue, "courseLevel", externalId)) {
                return null;
            }
            CourseLevel level = CourseLevel.fromApiValue(item.getCourseLevel());

            String sigun = item.getSigun();
            if (isFieldInvalid(sigun, "sigun", externalId)) {
                return null;
            }

            String subRegionName = sigun.split(WHITE_SPACE)[1];
            Area area = Area.findBySubRegion(subRegionName).orElseThrow(() -> {
                log.error("[두루누비 코스 동기화] 지역 파싱을 실패했습니다. subRegionName: {}", subRegionName);
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
     * Course 객체의 gpxPath에 있는 GPX 파일을 다운로드 및 파싱하여,
     * 해당 GPX 파일의 모든 좌표 정보를 TrackPoint 엔티티로 만들고 데이터베이스에 일괄 저장(Batch Insert)합니다.
     */
    public void saveTrackPoints(Course course) {
        long startTime = System.currentTimeMillis();
        log.info("[Course ID: {}] 트랙포인트 저장 시작", course.getId());

        try {
            // GPX 파일 다운로드
            String gpxContent = restTemplate.getForObject(course.getGpxPath(), String.class);
            if (gpxContent == null || gpxContent.isEmpty()) {
                log.warn("[Course ID: {}] GPX 파일이 비어있습니다. URL: {}", course.getId(), course.getGpxPath());
                return;
            }

            // Jackson을 사용한 XML 파싱
            GpxDto gpx = xmlMapper.readValue(gpxContent, GpxDto.class);

            List<GpxDto.Trkpt> allPoints = gpx.getTrk().getTrksegs().stream()
                    .flatMap(segment -> segment.getTrkpts().stream())
                    .toList();

            if (allPoints.isEmpty()) {
                log.warn("[Course ID: {}] GPX 데이터에 트랙포인트가 없습니다.", course.getId());
                return;
            }

            // 시작점 저장
            GpxDto.Trkpt firstPoint = allPoints.getFirst();
            Point startPoint = geometryFactory.createPoint(new Coordinate(firstPoint.getLon(), firstPoint.getLat()));
            course.setStartPoint(startPoint);

            // 고도값 및 트랙포인트 저장
            double minEle = Double.MAX_VALUE;
            double maxEle = Double.MIN_VALUE;
            List<TrackPoint> trackPointsToSave = new ArrayList<>();
            AtomicInteger sequence = new AtomicInteger(1);

            for (GpxDto.Trkpt point : allPoints) {
                // 최대,최소 고도 업데이트
                minEle = Math.min(minEle, point.getEle());
                maxEle = Math.max(maxEle, point.getEle());

                // 저장할 TrackPoint 엔티티 생성
                TrackPoint trackPoint = TrackPoint.builder()
                        .lat(point.getLat())
                        .lon(point.getLon())
                        .ele(point.getEle())
                        .sequence(sequence.getAndIncrement())
                        .build();
                trackPoint.setCourse(course);
                trackPointsToSave.add(trackPoint);
            }

            course.updateElevation(minEle, maxEle);
            trackPointRepository.saveAll(trackPointsToSave);
            courseRepository.save(course);

            long endTime = System.currentTimeMillis();
            log.info("[Course ID: {}] {}개의 트랙포인트 저장 완료. (소요 시간: {}ms)", course.getId(), trackPointsToSave.size(), (endTime - startTime));
        } catch (Exception e) {
            log.error("[Course ID: {}] 트랙포인트 동기화 중 오류 발생", course.getId(), e);
        }
    }

    /**
     * 코스의 길 상태(road_condition) 정보를 OpenAI API 호출을 통해 저장합니다.
     * 기존 데이터가 있는 경우, 일괄 삭제 후 새로 생성된 설명을 저장됩니다.
     * 하나의 코스 데이터 당 5개의 길 상태 정보가 생성됩니다.
     * 데이터 처리 과정을 로그로 확인할 수 있습니다.
     *
     * @param courseId 길 상태를 수정할 course Id
     */
    @Transactional
    public void updateRoadCondition(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
        List<TrackPoint> trackPoints = course.getTrackPoints();

        // 프롬프트 변수 준비
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

        // OpenAI API 호출 후 파싱 (응답은 "|"로 구분된 5개 설명으로 이루어짐)
        String openAiResponse = openAiService.getOpenAiResponse(getRoadConditionPrompt, variables);
        log.info("[길 상태 수정] OpenAI 응답 수신: {}", openAiResponse);
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
     * 데이터 저장 과정을 로그로 확인할 수 있습니다.
     *
     * @param courseImageFile 업로드된 이미지 파일
     */
    @Transactional
    public void updateCourseImage(Long courseId, MultipartFile courseImageFile) throws IOException {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 새 파일을 S3에 먼저 업로드
        String newImageUrl = fileService.uploadFile(courseImageFile, "image");
        log.info("[코스 이미지 수정] S3 버킷에 이미지 업로드 완료: {}", newImageUrl);

        // 삭제할 기존 파일 URL을 임시 변수에 저장
        String oldImageUrl = (course.getCourseImage() != null) ? course.getCourseImage().getImgUrl() : null;

        // DB 정보 업데이트
        if (oldImageUrl != null) {
            course.getCourseImage().updateImageUrl(newImageUrl);
        } else {
            course.updateCourseImage(new CourseImage(newImageUrl));
        }
        log.info("[코스 이미지 수정] DB에 이미지 정보 갱신 완료 (Course ID: {})", courseId);

        // 트랜잭션이 성공적으로 커밋된 후에 기존 파일 삭제
        if (oldImageUrl != null) {
            fileService.deleteFile(oldImageUrl);
            log.info("[코스 이미지 수정] S3에서 기존 이미지 삭제: Course Id={}, URL={}", course.getCourseImage().getCourseImageId(), course.getCourseImage().getImgUrl());
        }
    }

    /**
     * GPX 파일을 받아 코스 정보를 생성하고 저장합니다.
     * S3 버킷의 디렉토리는 "gpx"로 지정합니다.
     * 데이터 저장 과정을 로그로 확인할 수 있습니다.
     *
     * @param courseGpxFile 업로드된 GPX 파일
     */
    @Transactional
    public void createCourseToGpx(MultipartFile courseGpxFile) throws IOException {
        log.info("[GPX 코스 생성] 시작: 파일명={}, 크기={} bytes", courseGpxFile.getOriginalFilename(), courseGpxFile.getSize());

        // 1. GPX 파일의 track point 파싱
        List<TrackPoint> trackPoints;
        try {
            trackPoints = getTrackPoint(courseGpxFile);
            log.info("[GPX 코스 생성] 트랙포인트 파싱 완료 ({}개)", trackPoints.size());
        } catch (Exception e) {
            log.error("[GPX 코스 생성] 트랙포인트 파싱 실패", e);
            throw new BusinessException(GPX_FILE_PARSE_FAILED);
        }

        // 2. 전체 거리 계산
        int distance = calculateDistance(trackPoints);
        log.info("[GPX 코스 생성] 전체 거리 계산 완료: {}km", distance);

        // 3. 소요 시간 계산 (9km/h 속도 기준)
        int duration = calculateDuration(distance, 9.0);
        log.info("[GPX 코스 생성] 소요 시간 계산 완료: {}분", duration);

        // 4. 최대, 최소 고도 계산
        double maxElevation = calculateMaxElevation(trackPoints);
        double minElevation = calculateMinElevation(trackPoints);
        log.info("[GPX 코스 생성] 고도 계산 완료 (최대: {}, 최소: {})", maxElevation, minElevation);

        // 5. 시작점, 종료점 좌표 가져온 후, 주소값을 위해 카카오 지도 API 호출
        Point startPoint = extractStartPoint(trackPoints);
        Point endPoint = extractEndPoint(trackPoints);
        log.debug("[GPX 코스 생성] 시작점: {}, 종료점: {}", startPoint, endPoint);

        JsonNode startAddress = kakaoMapService.getAddressFromCoordinate(startPoint.getX(), startPoint.getY());
        JsonNode endAddress = kakaoMapService.getAddressFromCoordinate(endPoint.getX(), endPoint.getY());
        log.debug("[GPX 코스 생성] 시작점 주소: {}, 종료점 주소: {}", startAddress, endAddress);

        // 6. 코스 이름 가져오기
        // todo: 코스명 이름이 중복되는 경우 추가적인 처리 필요
        String courseName = extractCourseName(startAddress) + "~" + extractCourseName(endAddress);
        log.info("[GPX 코스 생성] 코스명 추출 완료: {}", courseName);

        // 7. 시작점을 기준으로 Area 분류
        Area area = extractArea(startAddress);
        log.info("[GPX 코스 생성] Area 분류 완료: {}", area);

        // 8. level, road condition을 위한 OpenAI API 호출 (응답은 "|"로 구분된 6개 설명으로 이루어짐)
        Map<String, Object> variables = Map.of(
                "name", courseName,
                "distance", distance,
                "duration", duration,
                "trackPoint", trackPoints
        );

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

        // 9. AWS S3에 GPX 파일 업로드
        String gpxPath = fileService.uploadFile(courseGpxFile, "gpx");

        // 10. course, road condition, track point DB에 저장
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

        courseRepository.save(course);
        log.info("[GPX 코스 생성] Course 저장 완료 (ID: {})", course.getId());

        List<RoadCondition> roadConditions = descriptions.stream()
                .map(description -> new RoadCondition(course, description))
                .toList();

        roadConditionRepository.saveAll(roadConditions);
        log.info("[GPX 코스 생성] RoadCondition {}개 저장 완료", roadConditions.size());

        for (TrackPoint trackPoint : trackPoints) {
            trackPoint.setCourse(course);
        }
        trackPointRepository.saveAll(trackPoints);
        log.info("[GPX 코스 생성] TrackPoint {}개 저장 완료", trackPoints.size());

        log.info("[GPX 코스 생성] 전체 작업 완료! (코스명: {})", courseName);
    }

    /**
     * GPX 파일에서 트랙포인트 목록을 추출합니다.
     * 추출할 때, 트랙(trk)이 있으면 우선적으로 파싱에 사용하고, 없으면 루트(rte)를 사용합니다.
     *
     * @param courseGpxFile 업로드된 GPX 파일
     * @return 추출된 TrackPoint 리스트 (lat, lon, ele, sequence)
     */
    private List<TrackPoint> getTrackPoint(MultipartFile courseGpxFile) throws Exception {
        XmlMapper xmlMapper = new XmlMapper();

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
     * 트랙포인트 리스트에서 종료점 좌표(endPoint)를 추출합니다.
     *
     * @param trackPoints 트랙포인트 리스트
     * @return 종료점 좌표 (Point), 없으면 null
     */
    private Point extractEndPoint(List<TrackPoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) return null;
        TrackPoint last = trackPoints.getLast();
        return geometryFactory.createPoint(new Coordinate(last.getLon(), last.getLat()));
    }

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 코스 이름을 추출합니다.
     *
     * @param jsonNode 주소 정보 JSON
     * @return 코스 이름 (지번 주소 조합(건물 이름)으로 구성, 없으면 "이름 없음")
     */
    private String extractCourseName(JsonNode jsonNode) {
        String courseName = null;

        // 지번 주소 조합 가져오기 (예: 기장읍 죽성리 30-35)
        JsonNode address = jsonNode.path("address"); // 지번 주소
        String dongName = address.path("region_3depth_name").asText(); // 동 단위
        String mainAddressNo = address.path("main_address_no").asText(); // 지번 주 번지
        String subAddressNo = address.path("sub_address_no").asText(); // 지번 부 번지, 없으면 빈 문자열("") 반환
        if (!dongName.isBlank() && !mainAddressNo.isBlank()) {
            courseName = dongName + " " + mainAddressNo + (subAddressNo.isBlank() ? "" : "-" + subAddressNo);
        } else {
            log.warn("[GPX 코스 생성] 코스 이름 추출 실패: dongName='{}', mainAddressNo='{}', subAddressNo='{}'. address: {}",
                    dongName, mainAddressNo, subAddressNo, address);
            return "이름 없음";
        }

        // 건물 이름 가져오기 (예: 무지개아파트)
        JsonNode roadAddress = jsonNode.path("road_address"); // 도로명 주소
        String buildingName = roadAddress.path("building_name").asText(); // 건물 이름
        if (buildingName != null && !buildingName.isBlank()) {
            return courseName + "(" + buildingName + ")";
        } else {
            return courseName;
        }
    }

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 행정구역(Area)을 추출합니다.
     * 도로명 주소(road_address)는 좌표에 따라 반환되지 않을 수 있기 때문에 지번 주소(address)를 기준으로 합니다.
     *
     * @param jsonNode 주소 정보 JSON
     * @return Area enum 값, 없으면 Area.UNKNOWN
     */
    private Area extractArea(JsonNode jsonNode) {
        String districtName = jsonNode.path("address").path("region_2depth_name").asText(); // 구 단위
        String dongName = jsonNode.path("address").path("region_3depth_name").asText(); // 동 단위

        // Area 설정
        for (Area area : Area.values()) {
            if (area.getSubRegions().contains(districtName) || area.getSubRegions().contains(dongName)) {
                return area;
            }
        }

        log.warn("[GPX 코스 생성] 행정구역 매칭 실패: districtName='{}', dongName='{}'. Area.UNKNOWN 반환", districtName, dongName);
        return Area.UNKNOWN;
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
            log.warn("[OpenAI 응답 파싱] 파싱 개수 부족. 기대치: {}, 실제: {}, 원본 응답: {}", parseNumber, parseData.size(), response);
        }

        return parseData;
    }
}
