package com.server.running_handai.domain.course.repository;

import com.server.running_handai.domain.course.entity.RoadCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadConditionRepository extends JpaRepository<RoadCondition, Long> {
    void deleteByCourseId(Long courseId);
}
