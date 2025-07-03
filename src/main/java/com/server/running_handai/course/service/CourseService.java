package com.server.running_handai.course.service;

import com.server.running_handai.course.dto.CourseInfoDto;
import com.server.running_handai.course.entity.Area;
import com.server.running_handai.course.entity.Theme;
import com.server.running_handai.course.repository.CourseRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    public static final String POINT_FORMAT = "POINT(%f %f)"; // MySQL의 POINT 포맷

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CourseInfoDto> findCoursesNearby(double lat, double lon) {
        log.info("사용자 근방 10km 이내 코스 조회를 시작합니다. lat={}, lon={}", lat, lon);
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtoList = courseRepository.findCoursesNearbyUser(userPoint);
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtoList.size());
        return courseInfoDtoList;
    }

    @Transactional(readOnly = true)
    public List<CourseInfoDto> findCoursesByArea(Area area, double lat, double lon) {
        log.info("지역 필터링 기반 코스 조회를 시작합니다. lat={}, lon={}, area={}", lat, lon, area.name());
        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtoList = courseRepository.findCoursesByArea(userPoint, area.name());
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtoList.size());
        return courseInfoDtoList;
    }

    @Transactional(readOnly = true)
    public List<CourseInfoDto> findCoursesByTheme(Theme theme, double lat, double lon) {
        log.info("테마 기반 코스 조회를 시작합니다. lat={}, lon={}, theme={}", lat, lon, theme.name());

        List<String> matchingAreaNames = Arrays.stream(Area.values()) // 모든 Area Enum 상수 가져옴
                .filter(area -> area.getTheme() == theme)   // theme이 일치하는 것만 필터링
                .map(Enum::name)                                 // Enum 상수의 이름 추출(ex. "HAEUN_GWANGAN")
                .toList();                                       // 리스트로 생성

        if (matchingAreaNames.isEmpty()) { // 일치하는 지역이 없으면 빈 리스트 즉시 반환
            return List.of();
        }

        String userPoint = String.format(POINT_FORMAT, lat, lon);
        List<CourseInfoDto> courseInfoDtoList = courseRepository.findCoursesInAreaList(userPoint, matchingAreaNames);
        log.info("{}개의 코스를 찾았습니다.", courseInfoDtoList.size());
        return courseInfoDtoList;
    }

}
