package com.server.running_handai.domain.course.repository;

import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.entity.Course;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 두루누비 데이터로 생성한 모든 Course 엔티티 조회 (csv로 생성한 Course 제외)
     */
    List<Course> findByExternalIdIsNotNull();

    /**
     * 코스의 시작점을 기준으로 사용자의 현재 위치에서 10km 이내에 있는 Course 목록 조회
     */
    @Query(
            value = "SELECT " +
                    "    c.course_id AS id, " +
                    "    c.name, " +
                    "    ci.img_url AS thumbnailUrl, " +
                    "    c.distance, " +
                    "    c.duration, " +
                    "    c.max_ele AS maxElevation, " +
                    "    (ST_Distance_Sphere(c.start_point, ST_PointFromText(:userPoint, 4326)) / 1000) AS distanceFromUser " +
                    "FROM " +
                    "    course c " +
                    "LEFT JOIN " +
                    "    course_image ci ON c.course_id = ci.course_id " +
                    "WHERE " +
                    "    ST_Distance_Sphere(c.start_point, ST_PointFromText(:userPoint, 4326)) <= 10000 " +
                    "ORDER BY " +
                    "    distanceFromUser ASC",
            nativeQuery = true
    )
    List<CourseInfoDto> findCoursesNearbyUser(@Param("userPoint") String userPoint);

    /**
     * 특정 지역 내의 Course 목록 조회
     */
    @Query(
            value = "SELECT " +
                    "    c.course_id AS id, " +
                    "    c.name, " +
                    "    ci.img_url AS thumbnailUrl, " +
                    "    c.distance, " +
                    "    c.duration, " +
                    "    c.max_ele AS maxElevation, " +
                    "    (ST_Distance_Sphere(c.start_point, ST_PointFromText(:userPoint, 4326)) / 1000) AS distanceFromUser " +
                    "FROM " +
                    "    course c " +
                    "LEFT JOIN " +
                    "    course_image ci ON c.course_id = ci.course_id " +
                    "WHERE c.area = :area " +
                    "ORDER BY distanceFromUser ASC",
            nativeQuery = true
    )
    List<CourseInfoDto> findCoursesByArea(@Param("userPoint") String userPoint, @Param("area") String area);

    /**
     * 특정 테마에 해당하는 Course 목록 조회
     */
    @Query(
            value = "SELECT " +
                    "    c.course_id AS id, " +
                    "    c.name, " +
                    "    ci.img_url AS thumbnailUrl, " +
                    "    c.distance, " +
                    "    c.duration, " +
                    "    c.max_ele AS maxElevation, " +
                    "    (ST_Distance_Sphere(c.start_point, ST_PointFromText(:userPoint, 4326)) / 1000) AS distanceFromUser " +
                    "FROM " +
                    "    course c " +
                    "LEFT JOIN " +
                    "    course_image ci ON c.course_id = ci.course_id " +
                    "JOIN " +
                    "    course_themes ct ON ct.course_course_id = c.course_id " +
                    "WHERE " +
                    "    ct.theme = :theme " +
                    "ORDER BY " +
                    "    distanceFromUser ASC",
            nativeQuery = true
    )
    List<CourseInfoDto> findCoursesByTheme(@Param("userPoint") String userPoint, @Param("theme") String theme);

    @Query("SELECT c FROM Course c " +
           "JOIN FETCH c.trackPoints " +
           "WHERE c.id = :courseId")
    Optional<Course> findCourseWithDetailsById(@Param("courseId") Long courseId);

    /**
     * Member가 생성한 Course 목록을 정렬 조건에 따라 조회
     */
    @Query(
            value = "SELECT " +
                    "    c.course_id AS id, " +
                    "    c.name, " +
                    "    ci.img_url AS thumbnailUrl, " +
                    "    c.distance, " +
                    "    c.duration, " +
                    "    c.max_ele AS maxElevation, " +
                    "    0.0 AS distanceFromUser " +
                    "FROM " +
                    "    course c " +
                    "LEFT JOIN " +
                    "    course_image ci ON c.course_id = ci.course_id " +
                    "WHERE c.member_id = :memberId ",
            nativeQuery = true
    )
    List<CourseInfoDto> findMyCoursesBySort(@Param("memberId") Long memberId, Sort sort);

    boolean existsByName(String name);

    /**
     * 사용자가 생성한 코스 조회
     */
    Optional<Course> findByIdAndCreatorId(Long courseId, Long memberId);

    /**
     * 사용자가 생성한 코스를 트랙 포인트와 함께 조회
     */
    @Query("SELECT c FROM Course c " +
            "LEFT JOIN FETCH c.trackPoints WHERE c.id = :courseId AND c.creator.id = :memberId")
    Optional<Course> findByIdAndCreatorIdWithTrackPoints(@Param("courseId") Long courseId, @Param("memberId") Long memberId);
}
