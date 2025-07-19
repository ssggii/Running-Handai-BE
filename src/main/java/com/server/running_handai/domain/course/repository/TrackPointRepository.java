package com.server.running_handai.domain.course.repository;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.entity.TrackPoint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
