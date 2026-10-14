package com.server.running_handai.domain.course.dto;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.CourseLevel;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.domain.course.entity.TrackPoint;
import java.util.List;
import org.locationtech.jts.geom.Point;

/**
 * GPX 파일과 외부 API(카카오, OpenAI, S3) 결과로 만든, DB 저장 전의 코스 정보입니다.
 */
public record GpxCourseDraft(
        String name,
        double distance,
        int duration,
        CourseLevel level,
        Area area,
        String gpxPath,
        Point startPoint,
        double maxElevation,
        double minElevation,
        List<Theme> themes,
        List<String> roadConditions,
        List<TrackPoint> trackPoints
) {
}
