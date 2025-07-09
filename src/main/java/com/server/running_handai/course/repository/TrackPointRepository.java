package com.server.running_handai.course.repository;

import com.server.running_handai.course.entity.TrackPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {

    /**
     * 특정 코스의 전체 트랙포인트 목록 조회
     */
    List<TrackPoint> findByCourseId(Long courseId);

}
