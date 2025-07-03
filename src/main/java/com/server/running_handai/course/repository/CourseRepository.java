package com.server.running_handai.course.repository;

import com.server.running_handai.course.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 두루누비 데이터로 생성한 모든 Course 엔티티 조회 (csv로 생성한 Course 제외)
     */
    List<Course> findByExternalIdIsNotNull();

    /**
     * gpxPath가 null이 아니면서 trackPoints 리스트가 비어있는 Course 목록을 조회합니다.
     * @return TrackPoint 저장이 필요한 Course 리스트
     */
    @Query("SELECT c FROM Course c WHERE c.gpxPath IS NOT NULL AND c.trackPoints IS EMPTY")
    List<Course> findCoursesWithEmptyTrackPoints();
}
