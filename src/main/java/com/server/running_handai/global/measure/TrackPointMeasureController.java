package com.server.running_handai.global.measure;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.TrackPoint;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.domain.course.service.CourseDataService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 트랙포인트 저장 구간만 따로 재는 측정 전용 API입니다. (measure 프로필에서만 등록)
 * OpenAI, 카카오, S3 호출 없이 CourseDataService의 저장 구간 메서드를 실제 코드 그대로 호출합니다.
 * 응답의 elapsedMs는 저장 구간 메서드 호출부터 flush까지(쓰기 지연된 SQL까지 DB에 보낸 시점)의 시간입니다.
 */
@Profile("measure")
@RestController
@RequestMapping("/measure/track-points")
@RequiredArgsConstructor
public class TrackPointMeasureController {

    private static final String COURSE_NAME_PREFIX = "measure-c-";

    private final CourseDataService courseDataService;
    private final CourseRepository courseRepository;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final GeometryFactory geometryFactory;

    /**
     * GPX 코스 생성과 같은 흐름: 새 코스를 저장한 뒤 같은 트랜잭션에서 트랙포인트를 저장합니다.
     * 시간은 트랙포인트 저장 구간만 잽니다. (코스 INSERT 1건은 시간에서 빠지고 쿼리 수에는 포함)
     */
    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam int points) {
        return transactionTemplate.execute(status -> {
            List<TrackPoint> trackPoints = generateTrackPoints(points);
            Course course = newCourse();
            courseRepository.save(course);

            long start = System.nanoTime();
            courseDataService.saveTrackPoints(course, trackPoints);
            entityManager.flush();
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            return Map.of("courseId", course.getId(), "points", points, "elapsedMs", elapsedMs);
        });
    }

    /**
     * 두루누비 동기화와 같은 흐름: 이미 저장된 코스를 읽고, 기존 트랙포인트를 지운 뒤 새 트랙포인트를 저장합니다.
     * 시간은 교체 구간만 잽니다. (코스 조회 SELECT 1건은 시간에서 빠지고 쿼리 수에는 포함)
     */
    @PostMapping("/replace")
    public Map<String, Object> replace(@RequestParam long courseId, @RequestParam int points) {
        return transactionTemplate.execute(status -> {
            List<TrackPoint> trackPoints = generateTrackPoints(points);
            Course course = courseRepository.findById(courseId).orElseThrow();

            long start = System.nanoTime();
            courseDataService.replaceTrackPoints(course, trackPoints);
            entityManager.flush();
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            return Map.of("courseId", courseId, "points", points, "elapsedMs", elapsedMs);
        });
    }

    private Course newCourse() {
        return Course.builder()
                .name(COURSE_NAME_PREFIX + UUID.randomUUID())
                .distance(5.0)
                .duration(33)
                .level(CourseLevel.MEDIUM)
                .area(Area.ETC)
                .gpxPath("measure")
                .startPoint(geometryFactory.createPoint(new Coordinate(129.1604, 35.1587)))
                .maxElevation(60.0)
                .minElevation(10.0)
                .build();
    }

    /**
     * 해운대해수욕장에서 북동쪽으로 이어지는 트랙포인트를 만듭니다. 값은 매번 같습니다. (sequence는 1부터)
     */
    private List<TrackPoint> generateTrackPoints(int count) {
        List<TrackPoint> trackPoints = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            trackPoints.add(TrackPoint.builder()
                    .lat(35.1587 + i * 0.00001)
                    .lon(129.1604 + i * 0.00001)
                    .ele(10 + i % 50)
                    .sequence(i + 1)
                    .build());
        }
        return trackPoints;
    }
}
