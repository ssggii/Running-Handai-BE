package com.server.running_handai.domain.course.repository;

import com.server.running_handai.domain.course.dto.CourseTrackPointDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.TrackPoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {

    /**
     * 특정 코스의 시작 포인트를 반환
     */
    Optional<TrackPoint> findFirstByCourseOrderBySequenceAsc(Course course);

    /**
     * 특정한 courseId의 트랙포인트 전체 삭제
     */
    void deleteByCourseId(Long courseId);

    /**
     * 특정 코스의 전체 트랙포인트 목록 조회
     */
    List<TrackPoint> findByCourseId(Long courseId);

    /**
     * 특정 코스의 트랙 포인트를 순서대로 조회
     */
    List<TrackPoint> findByCourseIdOrderBySequenceAsc(Long courseId);

    /**
     * 코스 ID 목록으로 모든 트랙포인트를 한 번에 조회
     */
    List<TrackPoint> findByCourseIdInOrderBySequenceAsc(List<Long> courseIds);

    /**
     * DB에 저장된 모든 코스별 시작점, 도착점 조회
     */
    @Query(
            value = "SELECT " +
                    "    c.course_id AS courseId, " +
                    "    stp.lat AS startPointLat, " +
                    "    stp.lon AS startPointLon, " +
                    "    etp.lat AS endPointLat, " +
                    "    etp.lon AS endPointLon " +
                    "FROM course c " +
                    "JOIN track_point stp ON c.course_id = stp.course_id AND stp.sequence = 1 " +
                    "JOIN track_point etp ON c.course_id = etp.course_id AND etp.sequence = (" +
                        "SELECT MAX(mtp.sequence) FROM track_point mtp WHERE mtp.course_id = c.course_id" +
                    ")",
            nativeQuery = true
    )
    List<CourseTrackPointDto> findAllCourseTrackPoint();
}
