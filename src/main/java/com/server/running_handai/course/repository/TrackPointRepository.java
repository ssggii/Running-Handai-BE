package com.server.running_handai.course.repository;

import com.server.running_handai.course.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackPointRepository extends JpaRepository<TrackPoint, Long> {
}
