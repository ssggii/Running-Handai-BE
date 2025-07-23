package com.server.running_handai.domain.course.service;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;

import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.course.dto.BookmarkInfo;
import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.dto.CourseWithPointDto;
import com.server.running_handai.domain.course.dto.TrackPointDto;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
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
public class CourseService {

    public static final String POINT_FORMAT = "POINT(%f %f)"; // MySQL의 POINT 포맷

    private final CourseRepository courseRepository;
    private final TrackPointRepository trackPointRepository;
    private final BookmarkRepository bookmarkRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Value("${course.simplification.distance-tolerance}")
    private double distanceTolerance;

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesNearby(double lat, double lon, Long memberId) {
        log.info("사용자 근방 10km 이내 코스 조회를 시작합니다. lat={}, lon={}", lat, lon);
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesNearbyUser(userPoint);
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos, memberId);
    }

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesByArea(Area area, double lat, double lon, Long memberId) {
        log.info("지역 필터링 기반 코스 조회를 시작합니다. lat={}, lon={}, area={}", lat, lon, area.name());
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesByArea(userPoint, area.name());
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos, memberId);
    }

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesByTheme(Theme theme, double lat, double lon, Long memberId) {
        log.info("테마 기반 코스 조회를 시작합니다. lat={}, lon={}, theme={}", lat, lon, theme.name());
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesByTheme(userPoint, theme.name());
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos, memberId);
    }

    private List<CourseWithPointDto> combineCoursesWithPoints(List<CourseInfoDto> courseInfos, Long memberId) {
        return courseInfos.stream()
                .map(courseInfo -> {
                    // 트랙포인트 데이터 결합
                    List<TrackPointDto> trackPoints = trackPointRepository.findByCourseId(courseInfo.getId())
                            .stream()
                            .map(TrackPointDto::from)
                            .toList();

                    // 북마크 관련 데이터 결합
                    int bookmarks = bookmarkRepository.countByCourseId(courseInfo.getId());
                    boolean isBookmarked = false;
                    if (memberId != null) {
                        isBookmarked = bookmarkRepository.existsByCourseIdAndMemberId(courseInfo.getId(), memberId);
                    }

                    return new CourseWithPointDto(
                            courseInfo.getId(),
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

    /**
     * 특정 코스의 상세정보를 조회합니다.
     * 코스 정보, 경로 좌표, 북마크 여부 등을 포함하며, N+1 문제 해결을 위해 Fetch Join을 사용합니다.
     *
     * @param courseId 조회하려는 코스의 ID
     * @param memberId 조회 요청한 회원 ID (비회원은 null)
     * @return 코스의 상세정보가 담긴 DTO
     * @throws BusinessException 코스를 찾지 못한 경우
     */
    @Transactional(readOnly = true)
    public CourseDetailDto findCourseDetails(Long courseId, Long memberId) {
        log.info("코스 상세정보 조회를 시작합니다. courseId: {}", courseId);
        Course course = findCourseByIdWithDetails(courseId);
        List<TrackPointDto> trackPoints = simplifyTrackPoints(course);
        BookmarkInfo bookmarkInfo = getBookmarkInfo(courseId, memberId);
        return CourseDetailDto.of(course, trackPoints, bookmarkInfo);
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
        log.info("트랙포인트 간소화 완료. courseId: {} (원본: {}개 → 단순화: {}개)",
                course.getId(), originalLine.getNumPoints(), simplifiedLine.getNumPoints());

        return Arrays.stream(simplifiedLine.getCoordinates())
                .map(TrackPointDto::from)
                .toList();
    }

    private BookmarkInfo getBookmarkInfo(Long courseId, Long memberId) {
        int totalBookmarks = bookmarkRepository.countByCourseId(courseId);
        boolean isBookmarkedByUser = (memberId != null) && bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId);
        return new BookmarkInfo(totalBookmarks, isBookmarkedByUser);
    }
}
