package com.server.running_handai.course.repository;

import com.server.running_handai.course.entity.Course;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * 두루누비 데이터로 생성한 모든 Course 엔티티 조회 (csv로 생성한 Course 제외)
     */
    List<Course> findByExternalIdIsNotNull();
}
