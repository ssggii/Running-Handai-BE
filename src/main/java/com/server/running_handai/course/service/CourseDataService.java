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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static com.server.running_handai.global.response.ResponseCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDataService {

    public static final String WHITE_SPACE = " ";
    public static final String TARGET_REGION = "부산";
    public static final int RUNNING_SPEED = 9;

    private final DurunubiApiClient durunubiApiClient;
    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private final RestTemplate restTemplate = new RestTemplate();
    private final XmlMapper xmlMapper = new XmlMapper();
    private final ChatClient.Builder chatClientBuilder;
    private final RoadConditionRepository roadConditionRepository;

    private final S3Client s3Client;
    private final KakaoAddressService kakaoAddressService;

    @Value("classpath:prompt/save-road-condition.st")
    private Resource getRoadConditionPrompt;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

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

    /** OpenAI API 관련 */
    /**
     * 길 상태 수정
     */
    @Transactional
    public RoadConditionResponseDto updateRoadCondition(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
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

        for (String descrption : descriptions) {
            RoadCondition rc = new RoadCondition(course, descrption);
            roadConditionRepository.save(rc);
        }

        return new RoadConditionResponseDto(course, descriptions);
    }

    /**
     * "|" 기준 응답값 파싱
     */
    private List<String> parseRoadConditionDescriptions(String response) {
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

    /**
     * 원하는 프롬프트로 OpenAI API 호출
     */
    public String callOpenAiApi(Resource promptResource, Map<String, Object> variables) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            PromptTemplate promptTemplate = new PromptTemplate(promptResource);
            Prompt prompt = promptTemplate.create(variables);

            return chatClient.prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new BusinessException(OPENAI_API_ERROR);
        }
    }

    /** AWS S3 관련 */
    /**
     * 코스 썸네일 이미지 업로드
     */
    @Transactional
    public CourseImageResponseDto updateCourseImage(Long courseId, MultipartFile courseImageFile) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
        String imageUrl;
        try {
            imageUrl = uploadFile(courseImageFile, "image");
        } catch (IOException e) {
            throw new BusinessException(FILE_UPLOAD_FAILED);
        }

        CourseImage courseImage = new CourseImage(imageUrl);
        course.updateCourseImage(courseImage);

        return new CourseImageResponseDto(course, courseImage);
    }

    /**
     * AWS S3에 파일 업로드
     * 같은 Bucket 내에 Directory로 구분
     */
    public String uploadFile(MultipartFile multipartFile, String directory) throws IOException {
        String originalFileName = multipartFile.getOriginalFilename();
        String fileName = directory + "/" + UUID.randomUUID() + "_" + originalFileName;

        // 업로드할 파일의 설정 정보 설정
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(multipartFile.getContentType())
                .build();

        // AWS S3에 파일 업로드
        s3Client.putObject(
                putObjectRequest,
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize())
        );

        // 업로드된 파일의 S3 Url 반환
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                region,
                fileName
        );
    }

    /** GPX 코스 추가 관련 */
    /**
     * GPX 코스 추가
     */
    @Transactional
    public CourseGpxResponseDto createCourseToGpx(MultipartFile courseGpxFile) {
        String gpxExternalId;
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        gpxExternalId = "A_CRS_MNG" + sb.toString();

        String gpxPath;
        try {
            gpxPath = uploadFile(courseGpxFile, "gpx");
        } catch (IOException e) {
            throw new BusinessException(FILE_UPLOAD_FAILED);
        }

        // 트랙포인트 파싱
        List<TrackPoint> trackPoints;
        try {
            trackPoints = getTrackPoint(courseGpxFile);
        } catch (Exception e) {
            throw new BusinessException(FILE_PARSE_FAILED);
        }

        // 전체 거리 계산
        int distance = calculateDistance(trackPoints);

        // 소요 시간 계산
        int duration = calculateDuration(distance, 9.0);

        // 최대, 최소 고도 계산
        double maxElevation = calculateMaxElevation(trackPoints);
        double minElevation = calculateMinElevation(trackPoints);

        // 시작점, 종료점 좌표 가져오기
        Point startPoint = extractStartPoint(trackPoints);
        Point endPoint = extractEndPoint(trackPoints);

        // 시작점, 종료점 카카오 지도 API로 주소 가져오기
        JsonNode startAddress = kakaoAddressService.getAddressFromCoordinate(startPoint.getX(), startPoint.getY());
        JsonNode endAddress = kakaoAddressService.getAddressFromCoordinate(endPoint.getX(), endPoint.getY());

        // 코스 이름 가져오기
        // todo: 코스명 이름이 중복되는 경우 추가적인 처리 필요
        String courseName = extractCourseName(startAddress) + "~" + extractCourseName(endAddress);

        // 시작점을 기준으로 Area 분류
        Area area = extractArea(startAddress);

        Course course = Course.builder()
                .externalId(gpxExternalId)
                .name(courseName)
                .distance(distance)
                .duration(duration)
                .level(CourseLevel.EASY)
                .area(area)
                .gpxPath(gpxPath)
                .startPoint(startPoint)
                .maxElevation(maxElevation)
                .minElevation(minElevation)
                .build();

        courseRepository.save(course);

        return new CourseGpxResponseDto(course);
    }

    /** GPX 트랙포인트 파싱 */
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

    /** distance (코스 전체 거리, 단위: km) 추정 */
    private static int calculateDistance(List<TrackPoint> trackPoints) {
        double totalDistance = 0.0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint previous = trackPoints.get(i - 1);
            TrackPoint current = trackPoints.get(i);
            totalDistance += haversine(previous.getLat(), previous.getLon(), current.getLat(), current.getLon());
        }
        return (int) Math.round(totalDistance);
    }

    /** 두 좌표 간 거리 계산 (하버사인 공식) */
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** duration (코스 소요 시간, 단위: 분) 추정 */
    private static int calculateDuration(double distance, double speed) {
        double hours = distance / speed;
        return (int) Math.round(hours * 60);
    }

    /** 최대 고도 계산 */
    private static double calculateMaxElevation(List<TrackPoint> trackPoints) {
        return trackPoints.stream().mapToDouble(TrackPoint::getEle).max().orElse(0.0);
    }

    /** 최소 고도 계산 */
    private static double calculateMinElevation(List<TrackPoint> trackPoints) {
        return trackPoints.stream().mapToDouble(TrackPoint::getEle).min().orElse(0.0);
    }

    /** 시작점 좌표 추출 */
    private Point extractStartPoint(List<TrackPoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) return null;
        TrackPoint first = trackPoints.getFirst();
        return geometryFactory.createPoint(new Coordinate(first.getLon(), first.getLat()));
    }

    /** 종료점 좌표 추출 */
    private Point extractEndPoint(List<TrackPoint> trackPoints) {
        if (trackPoints == null || trackPoints.isEmpty()) return null;
        TrackPoint last = trackPoints.getLast();
        return geometryFactory.createPoint(new Coordinate(last.getLon(), last.getLat()));
    }

    /** 코스 이름 추출 */
    private String extractCourseName(JsonNode jsonNode) {
        // building_name이 있을 경우 (예: 무지개아파트)
        JsonNode roadAddress = jsonNode.path("road_address"); // 도로명 주소
        String buildingName = roadAddress.path("building_name").asText(); // 건물 이름
        if (buildingName != null && !buildingName.isBlank()) return buildingName;

        // building_name이 없을 경우 (지번 주소 조합, 예: 기장읍 죽성리 30-35)
        JsonNode address = jsonNode.path("address"); // 지번 주소
        String region3 = address.path("region_3depth_name").asText(); // 동 단위
        String mainNo = address.path("main_address_no").asText(); // 지번 주 번지
        String subNo = address.path("sub_address_no").asText(); // 지번 부 번지, 없으면 빈 문자열("") 반환
        if (!region3.isBlank() && !mainNo.isBlank()) {
            return region3 + " " + mainNo + (subNo.isBlank() ? "" : "-" + subNo);
        }

        return "이름 없음";
    }

    /** 행정구역 추출 */
    private Area extractArea(JsonNode jsonNode) {
        // 도로명 주소는 도로명 주소는 좌표에 따라 반환되지 않을 수 있기 때문에 지번 주소를 기준으로 함
        String region2 = jsonNode.path("address").path("region_2depth_name").asText(); // 구 단위
        String region3 = jsonNode.path("address").path("region_3depth_name").asText(); // 동 단위

        // Area 설정
        for (Area area : Area.values()) {
            if (area.getSubRegions().contains(region2) || area.getSubRegions().contains(region3)) {
                return area;
            }
        }

        return Area.UNKNOWN;
    }
}
