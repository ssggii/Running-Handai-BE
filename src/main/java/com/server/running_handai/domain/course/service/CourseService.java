package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;
import static com.server.running_handai.global.response.ResponseCode.INVALID_AREA_PARAMETER;
import static com.server.running_handai.global.response.ResponseCode.INVALID_THEME_PARAMETER;

import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.course.dto.BookmarkCountDto;
import com.server.running_handai.domain.course.dto.BookmarkInfoDto;
import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.dto.CourseFilterRequestDto;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.dto.CourseInfoWithDetailsDto;
import com.server.running_handai.domain.course.dto.TrackPointDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    public static final String MYSQL_POINT_FORMAT = "POINT(%f %f)";

    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final BookmarkRepository bookmarkRepository;
    private final GeometryFactory geometryFactory;

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
        Map<Long, List<TrackPointDto>> trackPointMap = getTrackPointMap(courseIds);
        Map<Long, Long> bookmarkCountMap = getBookmarkCountMap(courseIds);
        Set<Long> bookmarkedCourseIds = getBookmarkedCourseIds(memberId, courseIds);

        return courseInfos.stream()
                .map(courseInfo -> {
                    long courseId = courseInfo.getId();
                    List<TrackPointDto> trackPoints = trackPointMap.getOrDefault(courseId, Collections.emptyList());
                    int bookmarks = bookmarkCountMap.getOrDefault(courseId, 0L).intValue();
                    boolean isBookmarked = bookmarkedCourseIds.contains(courseId);

                    return new CourseInfoWithDetailsDto(
                            courseId,
                            courseInfo.getThumbnailUrl(),
                            courseInfo.getDistance(),
                            courseInfo.getDuration(),
                            courseInfo.getMaxElevation(),
                            courseInfo.getDistanceFromUser(),
                            bookmarks,
                            isBookmarked,
                            trackPoints
                    );
                })
                .toList();
    }

    private Map<Long, List<TrackPointDto>> getTrackPointMap(List<Long> courseIds) {
        return trackPointRepository.findByCourseIdInOrderBySequenceAsc(courseIds)
                .stream()
                .collect(
                        Collectors.groupingBy(
                                trackPoint -> trackPoint.getCourse().getId(),
                                Collectors.mapping(TrackPointDto::from, Collectors.toList())
                        ));
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
        List<TrackPointDto> trackPoints = simplifyTrackPoints(course);
        BookmarkInfoDto bookmarkInfoDto = getBookmarkInfo(courseId, memberId);
        return CourseDetailDto.of(course, trackPoints, bookmarkInfoDto);
    }

    private Course findCourseByIdWithDetails(Long courseId) {
        return courseRepository.findCourseWithDetailsById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));
    }

    private List<TrackPointDto> simplifyTrackPoints(Course course) {
        Coordinate[] originalCoordinates = course.getTrackPoints().stream()
                .map(point -> new Coordinate(point.getLon(), point.getLat(), point.getEle()))
                .toArray(Coordinate[]::new);

        LineString originalLine = geometryFactory.createLineString(originalCoordinates);
        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(originalLine); // RDP 알고리즘 사용하여 경로 단순화
        simplifier.setDistanceTolerance(distanceTolerance); // 이 값이 클수록 더 많이 단순화됨 (0.0001은 약 10m에 해당)
        LineString simplifiedLine = (LineString) simplifier.getResultGeometry();
        log.info("[트랙포인트 간소화] courseId: {} (원본: {}개 → 단순화: {}개)",
                course.getId(), originalLine.getNumPoints(), simplifiedLine.getNumPoints());

        return Arrays.stream(simplifiedLine.getCoordinates())
                .map(TrackPointDto::from)
                .toList();
    }

    private BookmarkInfoDto getBookmarkInfo(Long courseId, Long memberId) {
        int totalBookmarks = bookmarkRepository.countByCourseId(courseId);
        boolean isBookmarkedByUser = (memberId != null) && bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId);
        return new BookmarkInfoDto(totalBookmarks, isBookmarkedByUser);
    }
}
