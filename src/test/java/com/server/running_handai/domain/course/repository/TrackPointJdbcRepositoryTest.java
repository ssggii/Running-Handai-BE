package com.server.running_handai.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.TrackPoint;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(TrackPointJdbcRepository.class)
class TrackPointJdbcRepositoryTest {

    // 배치 크기(1,000)를 넘는 개수
    private static final int POINT_COUNT = 2500;

    @Autowired
    private TrackPointJdbcRepository trackPointJdbcRepository;

    @Autowired
    private TrackPointRepository trackPointRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("트랙포인트를 JDBC 배치로 저장하면 모든 포인트가 순서와 값 그대로, 생성·수정 시각과 함께 저장된다")
    void batchInsertSavesAllPointsInOrder() {
        // given
        Course course = courseRepository.save(newCourse("course1"));
        List<TrackPoint> trackPoints = trackPoints(POINT_COUNT);

        // when
        trackPointJdbcRepository.batchInsert(course.getId(), trackPoints);

        // then
        List<TrackPoint> saved = trackPointRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        assertThat(saved).hasSize(POINT_COUNT);
        for (int i = 0; i < POINT_COUNT; i++) {
            TrackPoint expected = trackPoints.get(i);
            TrackPoint actual = saved.get(i);
            assertThat(actual.getSequence()).isEqualTo(expected.getSequence());
            assertThat(actual.getLat()).isEqualTo(expected.getLat());
            assertThat(actual.getLon()).isEqualTo(expected.getLon());
            assertThat(actual.getEle()).isEqualTo(expected.getEle());
        }

        Integer missingTimestamps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM track_point WHERE course_id = ? AND (created_at IS NULL OR updated_at IS NULL)",
                Integer.class, course.getId());
        assertThat(missingTimestamps).isZero();
    }

    @Test
    @DisplayName("벌크 삭제는 해당 코스의 트랙포인트만 모두 지운다")
    void bulkDeleteRemovesOnlyTargetCourse() {
        // given
        Course target = courseRepository.save(newCourse("target"));
        Course other = courseRepository.save(newCourse("other"));
        trackPointJdbcRepository.batchInsert(target.getId(), trackPoints(POINT_COUNT));
        trackPointJdbcRepository.batchInsert(other.getId(), trackPoints(10));

        // when
        int deleted = trackPointRepository.deleteAllByCourseIdInBulk(target.getId());

        // then
        assertThat(deleted).isEqualTo(POINT_COUNT);
        assertThat(countTrackPoints(target.getId())).isZero();
        assertThat(countTrackPoints(other.getId())).isEqualTo(10);
    }

    private int countTrackPoints(Long courseId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM track_point WHERE course_id = ?", Integer.class, courseId);
    }

    private Course newCourse(String name) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        return Course.builder()
                .name(name)
                .distance(5.0)
                .duration(33)
                .level(CourseLevel.MEDIUM)
                .area(Area.ETC)
                .gpxPath("test")
                .startPoint(geometryFactory.createPoint(new Coordinate(129.1604, 35.1587)))
                .maxElevation(60.0)
                .minElevation(10.0)
                .build();
    }

    private List<TrackPoint> trackPoints(int count) {
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
