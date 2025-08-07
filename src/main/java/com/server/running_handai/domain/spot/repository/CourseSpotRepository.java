package com.server.running_handai.domain.spot.repository;

import com.server.running_handai.domain.spot.entity.CourseSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, Long> {
    @Modifying
    @Query("DELETE FROM CourseSpot cs WHERE cs.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}