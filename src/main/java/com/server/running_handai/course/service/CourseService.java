package com.server.running_handai.course.service;

import static com.server.running_handai.global.response.ResponseCode.COURSE_NOT_FOUND;

import com.server.running_handai.bookmark.repository.BookmarkRepository;
import com.server.running_handai.course.dto.CourseDetailDto;
import com.server.running_handai.course.dto.CourseInfoDto;
import com.server.running_handai.course.dto.CourseWithPointDto;
import com.server.running_handai.course.dto.TrackPointDto;
import com.server.running_handai.course.entity.Area;
import com.server.running_handai.course.entity.Course;
import com.server.running_handai.course.entity.RoadCondition;
import com.server.running_handai.course.entity.Theme;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.course.repository.TrackPointRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
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

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesNearby(double lat, double lon) {
        log.info("사용자 근방 10km 이내 코스 조회를 시작합니다. lat={}, lon={}", lat, lon);
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesNearbyUser(userPoint);
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos);
    }

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesByArea(Area area, double lat, double lon) {
        log.info("지역 필터링 기반 코스 조회를 시작합니다. lat={}, lon={}, area={}", lat, lon, area.name());
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesByArea(userPoint, area.name());
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos);
    }

    @Transactional(readOnly = true)
    public List<CourseWithPointDto> findCoursesByTheme(Theme theme, double lat, double lon) {
        log.info("테마 기반 코스 조회를 시작합니다. lat={}, lon={}, theme={}", lat, lon, theme.name());
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtos = courseRepository.findCoursesByTheme(userPoint, theme.name());
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtos.size());
        return combineCoursesWithPoints(courseInfoDtos);
    }

    private List<CourseWithPointDto> combineCoursesWithPoints(List<CourseInfoDto> courseInfos) {

        return courseInfos.stream()
                .map(courseInfo -> {
                    List<TrackPointDto> trackPoints = trackPointRepository.findByCourseId(courseInfo.getId())
                            .stream()
                            .map(TrackPointDto::from)
                            .toList();
                    int bookmarks = bookmarkRepository.countByCourseId(courseInfo.getId());
                    return new CourseWithPointDto(
                            courseInfo.getId(),
                            courseInfo.getThumbnailUrl(),
                            courseInfo.getDistance(),
                            courseInfo.getDuration(),
                            courseInfo.getMaxElevation(),
                            courseInfo.getDistanceFromUser(),
                            bookmarks,
                            trackPoints
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailDto findCourseDetails(Long courseId) {
        log.info("코스 상세정보 조회를 시작합니다. courseId: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(COURSE_NOT_FOUND));

        List<String> roadConditions = course.getRoadConditions().stream()
                .map(RoadCondition::getDescription).toList();

        Coordinate[] originalCoordinates = course.getTrackPoints().stream()
                .map(point -> new Coordinate(point.getLon(), point.getLat(), point.getEle()))
                .toArray(Coordinate[]::new);

        LineString originalLine = geometryFactory.createLineString(originalCoordinates);
        DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(originalLine); // 경로 단순화 (RDP 알고리즘 적용)
        simplifier.setDistanceTolerance(0.0001); // 이 값이 클수록 더 많이 단순화됨. 0.0001은 약 10m에 해당
        LineString simplifiedLine = (LineString) simplifier.getResultGeometry();
        log.info("[Course ID: {}] 트랙포인트 간소화 완료. 원본: {}개 -> 단순화: {}개",
                courseId, originalLine.getNumPoints(), simplifiedLine.getNumPoints());

        List<TrackPointDto> simplifiedTrackPoints = Arrays.stream(simplifiedLine.getCoordinates())
                .map(c -> new TrackPointDto(c.getY(), c.getX(), c.getZ()))
                .toList();

        int bookmarks = bookmarkRepository.countByCourseId(courseId);

        return new CourseDetailDto(course.getId(), course.getDistance(), course.getDuration(),
                course.getMinElevation(), course.getMaxElevation(), course.getLevel().getDescription(),
                bookmarks, roadConditions,  simplifiedTrackPoints);
    }

}
