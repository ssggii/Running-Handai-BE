package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.DUPLICATE_COURSE_NAME;
import static com.server.running_handai.global.response.ResponseCode.INVALID_AREA_PARAMETER;
import static com.server.running_handai.global.response.ResponseCode.INVALID_THEME_PARAMETER;
import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.NO_AUTHORITY_TO_DELETE_COURSE;
import static com.server.running_handai.global.response.ResponseCode.NO_AUTHORITY_TO_UPDATE_COURSE;

import com.fasterxml.jackson.databind.JsonNode;
import com.server.running_handai.domain.bookmark.dto.BookmarkCountDto;
import com.server.running_handai.domain.bookmark.dto.BookmarkInfoDto;
import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.course.dto.*;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.event.CourseCreatedEvent;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.domain.review.dto.ReviewInfoDto;
import com.server.running_handai.domain.review.repository.ReviewRepository;
import com.server.running_handai.domain.review.service.ReviewService;
import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.repository.SpotRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;

import java.util.*;
import java.util.stream.Collectors;

import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    public static final String MYSQL_POINT_FORMAT = "POINT(%f %f)";
    public static final String COURSE_NAME_DELIMITER = "-";

    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReviewRepository reviewRepository;
    private final SpotRepository spotRepository;
    private final MemberRepository memberRepository;
    private final GeometryFactory geometryFactory;
    private final ReviewService reviewService;
    private final FileService fileService;
    private final CourseDataService courseDataService;
    private final KakaoMapService kakaoMapService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${course.simplification.distance-tolerance}")
    private double distanceTolerance;

    /**
     * 필터링 조건에 맞는 코스를 전체 조회합니다.
     *
     * @param filterOption 필터링 조건
     * @param memberId 조회 요청한 회원 ID (비회원은 null)
     * @return 조회된 코스 목록 DTO
     * @throws BusinessException Area 또는 Theme가 null인 경우
     */
    public List<CourseInfoWithDetailsDto> findCourses(CourseFilterRequestDto filterOption, Long memberId) {
        List<CourseInfoDto> courseInfos = switch (filterOption.filter()) {
            case NEARBY -> findCoursesNearby(filterOption);
            case AREA -> findCoursesByArea(filterOption);
            case THEME -> findCoursesByTheme(filterOption);
        };
        log.info("[코스 전체조회] Filter: {}, Member ID: {}, 조회된 코스 수: {}", filterOption, memberId, courseInfos.size());
        return buildCourseWithDetails(courseInfos, memberId);
    }

    private List<CourseInfoDto> findCoursesNearby(CourseFilterRequestDto request) {
        return courseRepository.findCoursesNearbyUser(parseUserPoint(request));
    }

    private List<CourseInfoDto> findCoursesByArea(CourseFilterRequestDto request) {
        if (request.area() == null) {
            throw new BusinessException(INVALID_AREA_PARAMETER);
        }
        return courseRepository.findCoursesByArea(parseUserPoint(request), request.area().name());
    }

    private List<CourseInfoDto> findCoursesByTheme(CourseFilterRequestDto request) {
        if (request.theme() == null) {
            throw new BusinessException(INVALID_THEME_PARAMETER);
        }
        return courseRepository.findCoursesByTheme(parseUserPoint(request), request.theme().name());
    }

    private String parseUserPoint(CourseFilterRequestDto request) {
        return String.format(MYSQL_POINT_FORMAT, request.lat(), request.lon());
    }

    private List<CourseInfoWithDetailsDto> buildCourseWithDetails(List<CourseInfoDto> courseInfos, Long memberId) {
        if (courseInfos.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> courseIds = courseInfos.stream().map(CourseInfoDto::getId).toList();
        Map<Long, List<TrackPointDto>> trackPointMap = getSimplifiedTrackPointMap(courseIds);
        Map<Long, Long> bookmarkCountMap = getBookmarkCountMap(courseIds);
        Set<Long> bookmarkedCourseIds = getBookmarkedCourseIds(memberId, courseIds);

        return courseInfos.stream()
                .map(courseInfo -> {
                    long courseId = courseInfo.getId();
                    List<TrackPointDto> trackPoints = trackPointMap.getOrDefault(courseId, Collections.emptyList());
                    int bookmarks = bookmarkCountMap.getOrDefault(courseId, 0L).intValue();
                    boolean isBookmarked = bookmarkedCourseIds.contains(courseId);
                    return CourseInfoWithDetailsDto.from(courseInfo, trackPoints, bookmarks, isBookmarked);
                })
                .toList();
    }

    private Map<Long, List<TrackPointDto>> getSimplifiedTrackPointMap(List<Long> courseIds) {
        Map<Long, List<TrackPoint>> trackPointMap = trackPointRepository.findByCourseIdInOrderBySequenceAsc(courseIds)
                .stream()
                .collect(Collectors.groupingBy(trackPoint -> trackPoint.getCourse().getId()));

        return trackPointMap.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey, // courseId
                                entry -> simplifyTrackPoints(entry.getValue()) // List<TrackPointDto>
                        )
                );
    }

    private Map<Long, Long> getBookmarkCountMap(List<Long> courseIds) {
        return bookmarkRepository.countByCourseIdIn(courseIds)
                .stream()
                .collect(Collectors.toMap(
                        BookmarkCountDto::courseId,
                        BookmarkCountDto::bookmarkCount
                ));
    }

    private Set<Long> getBookmarkedCourseIds(Long memberId, List<Long> courseIds) {
        return (memberId != null)
                ? bookmarkRepository.findBookmarkedCourseIdsByMember(courseIds, memberId)
                : Collections.emptySet();
    }

    /**
     * 특정 코스의 상세정보를 조회합니다.
     * 상세 정보에는 코스 정보, 경로 좌표, 북마크 여부 등을 포함합니다.
     *
     * @param courseId 조회하려는 코스의 ID
     * @param memberId 조회 요청한 회원 ID (비회원은 null)
     * @return 코스의 상세정보가 담긴 DTO
     * @throws BusinessException 코스를 찾지 못한 경우
     */
    public CourseDetailDto findCourseDetails(Long courseId, Long memberId) {
        Course course = findCourseByIdWithDetails(courseId);
        List<TrackPointDto> trackPoints = simplifyTrackPoints(course.getTrackPoints());
        BookmarkInfoDto bookmarkInfoDto = getBookmarkInfo(courseId, memberId);
        return CourseDetailDto.from(course, trackPoints, bookmarkInfoDto);
    }

    private Course findCourseByIdWithDetails(Long courseId) {
        return courseRepository.findCourseWithDetailsById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
    }

    private List<TrackPointDto> simplifyTrackPoints(List<TrackPoint> trackPoints) {
        if (trackPoints.isEmpty()) {
            return Collections.emptyList();
        }

        Coordinate[] originalCoordinates = trackPoints.stream()
                .map(point -> new Coordinate(point.getLon(), point.getLat(), point.getEle()))
                .toArray(Coordinate[]::new);

        LineString originalLine = geometryFactory.createLineString(originalCoordinates);
        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(originalLine); // RDP 알고리즘 사용하여 경로 단순화
        simplifier.setDistanceTolerance(distanceTolerance); // 이 값이 클수록 더 많이 단순화됨 (0.0001은 약 10m에 해당)
        LineString simplifiedLine = (LineString) simplifier.getResultGeometry();
        log.info("[트랙포인트 간소화] 원본: {}개 → 단순화: {}개)", originalLine.getNumPoints(), simplifiedLine.getNumPoints());

        return Arrays.stream(simplifiedLine.getCoordinates())
                .map(TrackPointDto::from)
                .toList();
    }

    private BookmarkInfoDto getBookmarkInfo(Long courseId, Long memberId) {
        int totalBookmarks = bookmarkRepository.countByCourseId(courseId);
        boolean isBookmarkedByUser = (memberId != null) && bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId);
        return new BookmarkInfoDto(totalBookmarks, isBookmarkedByUser);
    }

    /**
     * 상세-전체 탭을 위한 코스 요약 정보를 조회합니다.
     * 코스 요약 정보는 기본적인 코스 정보(전체 길이, 소요 시간, 최대 고도)와 즐길거리 및 리뷰를 포함합니다.
     *
     * @param courseId 조회하려는 코스의 ID
     * @param memberId 조회 요청한 회원 ID (비회원은 null)
     * @return 코스의 요약 정보가 담긴 DTO
     * @throws BusinessException 코스를 찾지 못한 경우
     */
    public CourseSummaryDto getCourseSummary(Long courseId, Long memberId) {
        // 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 리뷰 조회
        List<ReviewInfoDto> reviewInfoDtos = reviewService.convertToReviewInfoDtos(
                reviewRepository.findRecent2ByCourseId(courseId), memberId);
        int reviewCount = (int) reviewRepository.countByCourseId(courseId); // 리뷰 전체 개수
        double starAverage = reviewService.calculateAverageStars(courseId); // 리뷰 전체 평점

        // 즐길거리 조회
        List<SpotInfoDto> spotInfoDtos = spotRepository.findRandom3ByCourseId(course.getId());

        return CourseSummaryDto.from(course, reviewCount, starAverage, reviewInfoDtos, spotInfoDtos);
    }

    /**
     * 사용자가 생성한 코스의 GPX 파일 다운로드를 위한 Presigned GET URL을 발급합니다.
     * 해당 URL의 유효시간은 1시간입니다.
     *
     * @param courseId 다운로드하려는 코스 ID
     * @param memberId 다운로드 요청한 회원 ID
     * @return GPX 파일 다운로드용 Presigned GET URL이 포함된 DTO
     */
    public GpxPathDto downloadGpx(Long courseId, Long memberId) {
        Course course = courseRepository.findByIdAndCreatorId(courseId, memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        // Presigned GET URL 발급 (1시간)
        String gpxPath = fileService.getPresignedGetUrl(course.getGpxPath(), 60);

        return GpxPathDto.from(courseId, gpxPath);
    }

    /**
     * 사용자가 생성한 코스 목록을 페이징, 정렬 조건, 검색 키워드에 따라 조회합니다.
     *
     * @param memberId 조회 요청한 회원 ID
     * @param pageable 정렬 조건을 포함한 페이징 객체
     * @param keyword 검색 키워드 (코스 이름)
     * @return 정렬된 코스 목록이 포함된 DTO
     */
    public MyAllCoursesDetailDto getMyAllCourses(Long memberId, Pageable pageable, String keyword) {
        List<MyCourseInfoDto> courseInfoDtos = courseRepository.findMyCoursesWithPagingAndKeyword(memberId, pageable, keyword)
                .getContent().stream()
                .map(MyCourseInfoDto::from)
                .toList();
        return MyAllCoursesDetailDto.from(courseInfoDtos);
    }

    /**
     * 사용자가 생성한 코스를 조회합니다.
     *
     * @param memberId 조회 요청한 회원 ID
     * @param courseId 조회 요청한 코스 ID
     * @return 코스 정보가 포함된 DTO
     */
    public MyCourseDetailDto getMyCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findByIdAndCreatorIdWithTrackPoints(courseId, memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        List<TrackPointDto> trackPointDtos = course.getTrackPoints().stream()
                .map(TrackPointDto::from)
                .toList();

        return MyCourseDetailDto.from(course, trackPointDtos);
    }
    /**
     * 주어진 좌표가 부산 내에 있는지 판별합니다.
     *
     * @param longitude 경도 (x)
     * @param latitude  위도 (y)
     * @return 부산 지역 내에 있으면 true, 아니면 false
     */
    public boolean isInsideBusan(double longitude, double latitude) {
        JsonNode addressNode = kakaoMapService.getAddressFromCoordinate(longitude, latitude);

        if (addressNode == null) {
            log.warn("[지역 판별] 주소 정보를 찾을 수 없어 부산 외 지역으로 판별합니다: x={}, y={}", longitude, latitude);
            return false;
        }

        String city = addressNode.path("address").path("region_1depth_name").asText();
        log.info("[지역 판별] city={}", city);

        return city.startsWith("부산");
    }

    /**
     * 회원이 생성한 코스를 저장하고, 트랜잭션 커밋 후 이벤트를 발행합니다.
     *
     * @param memberId 요청 회원의 ID
     * @param request 코스 생성에 필요한 데이터 DTO
     * @return 저장된 코스의 ID
     */
    @Transactional
    public Long createMemberCourse(Long memberId, CourseCreateRequestDto request) {
        String newCourseName = request.startPointName().trim() + COURSE_NAME_DELIMITER + request.endPointName().trim();
        checkCourseNameDuplicated(newCourseName);
        Course newCourse = saveMemberCourse(memberId, request);
        courseDataService.updateCourseImage(newCourse.getId(), request.thumbnailImage());
        publishCourseCreatedEvent(newCourse.getId(), request.isInsideBusan());
        return newCourse.getId();
    }

    private void checkCourseNameDuplicated(String newCourseName) {
        if (courseRepository.existsByName(newCourseName)) {
            throw new BusinessException(DUPLICATE_COURSE_NAME);
        }
    }

    private Course saveMemberCourse(Long memberId, CourseCreateRequestDto request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
        Course newCourse = courseDataService.createCourseToGpx(
                new GpxCourseRequestDto(request.startPointName(), request.endPointName()), request.gpxFile());
        newCourse.setCreator(member);
        return newCourse;
    }

    private void publishCourseCreatedEvent(Long courseId, boolean isInsideBusan) {
        CourseCreatedEvent event = new CourseCreatedEvent(courseId, isInsideBusan);
        log.info("코스 생성 이벤트 발행. courseId: {}, isInsideBusan: {}", courseId, isInsideBusan);
        eventPublisher.publishEvent(event);
    }

    /**
     * 회원이 생성한 코스를 삭제합니다.
     * 코스에 저장된 즐길거리가 있는 경우 함께 삭제합니다.
     *
     * @param memberId 요청 회원의 ID
     * @param courseId 삭제하려는 코스의 ID
     */
    @Transactional
    public void deleteMemberCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 권한 검증 (요청자와 코스 생성자가 동일인인지 확인)
        if (course.getCreator() == null || !course.getCreator().getId().equals(memberId)) {
            throw new BusinessException(NO_AUTHORITY_TO_DELETE_COURSE);
        }

        fileService.deleteFile(course.getGpxPath()); // s3에서 gpx 파일 삭제
        fileService.deleteFile(course.getCourseImage().getImgUrl()); // s3에서 썸네일 이미지 삭제

        course.removeCreator();
        courseRepository.delete(course);
    }

    /**
     * 회원이 생성한 코스의 일부 정보를 수정합니다.
     *
     * @param memberId 요청 회원의 ID
     * @param courseId 수정하려는 코스의 ID
     * @param request  수정할 데이터가 담긴 DTO
     */
    @Transactional
    public void updateCourse(Long memberId, Long courseId, CourseUpdateRequestDto request) {
        // 코스 조회
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        // 권한 검사
        if (course.getCreator() == null || !course.getCreator().getId().equals(memberId)) {
            throw new BusinessException(NO_AUTHORITY_TO_UPDATE_COURSE);
        }

        // 필드 업데이트
        updateCourseName(course, request.startPointName(), request.endPointName());
        updateThumbnailImage(course, request.thumbnailImage());
    }

    private void updateCourseName(Course course, String newStartPointName, String newEndPointName) {
        // 시작 및 종료 포인트 이름 결정
        String updatedStartPointName = getUpdatedValue(newStartPointName,
                course.getName().split(COURSE_NAME_DELIMITER)[0]);
        String updatedEndPointName = getUpdatedValue(newEndPointName,
                course.getName().split(COURSE_NAME_DELIMITER)[1]);

        // 새로운 코스 이름 생성 및 변경 여부 확인
        String updatedCourseName = updatedStartPointName + COURSE_NAME_DELIMITER + updatedEndPointName;
        boolean isCourseNameChanged = !updatedCourseName.equals(course.getName());

        // 변경되었다면 중복 검사 후 이름 업데이트
        if (isCourseNameChanged) {
            checkCourseNameDuplicated(updatedCourseName);
            course.updateName(updatedCourseName);
        }
    }

    private String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue.trim() : oldValue;
    }

    private void updateThumbnailImage(Course course, MultipartFile newImageFile) {
        if (newImageFile != null && !newImageFile.isEmpty()) {
            courseDataService.updateCourseImage(course.getId(), newImageFile);
        }
    }
}
