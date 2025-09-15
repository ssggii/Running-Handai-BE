package com.server.running_handai.domain.spot.repository;

import com.server.running_handai.domain.spot.entity.CourseSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, Long> {
    /**
     * 특정 코스에 연결된 모든 장소의 연관관계를 모두 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM CourseSpot cs WHERE cs.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    /**
     * 특정 코스에 연결된 장소 중 External Id의 목록에 포함된 장소의 연관관계만 삭제합니다.
     */
    @Modifying
    @Query("DELETE FROM CourseSpot cs WHERE cs.course.id = :courseId AND cs.spot.externalId IN :externalIds")
    void deleteByCourseIdAndSpotExternalIdIn(@Param("courseId") Long courseId, @Param("externalIds") Set<String> externalIds);
}