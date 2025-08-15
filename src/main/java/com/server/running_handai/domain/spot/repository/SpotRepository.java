package com.server.running_handai.domain.spot.repository;

import com.server.running_handai.domain.spot.dto.SpotInfoDto;
import com.server.running_handai.domain.spot.entity.Spot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SpotRepository extends JpaRepository<Spot, Long> {
    /**
     * DB에 동일한 ExternalId를 가진 Spot이 있다면 해당 Spot을 가져옵니다.
     */
    List<Spot> findByExternalIdIn(Collection<String> externalIds);

    /**
     * CourseId와 일치하는 Spot을 SpotImage와 함께 가져옵니다.
     */
    @Query("SELECT s " +
            "FROM Spot s " +
            "LEFT JOIN FETCH s.spotImage " +
            "JOIN CourseSpot cs ON cs.spot = s " +
            "WHERE cs.course.id = :courseId")
    List<Spot> findByCourseIdWithSpotImage(@Param("courseId") Long courseId);

    /**
     * CourseId와 일치하는 Spot을 SpotImage와 함께 랜덤으로 3개 가져옵니다.
     */
    @Query(value = "SELECT " +
            "    s.spot_id AS spotId, " +
            "    s.name, " +
            "    s.description, " +
            "    si.img_url As imageUrl " +
            "FROM spot s " +
            "LEFT JOIN spot_image si ON s.spot_id = si.spot_id " +
            "JOIN course_spot cs ON cs.spot_id = s.spot_id " +
            "WHERE cs.course_id = :courseId " +
            "ORDER BY RAND() LIMIT 3",
            nativeQuery = true)
    List<SpotInfoDto> findRandom3ByCourseId(@Param("courseId") Long courseId);
}