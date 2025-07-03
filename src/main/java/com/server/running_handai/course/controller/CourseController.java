package com.server.running_handai.course.controller;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.course.dto.CourseInfoDto;
import com.server.running_handai.course.entity.Area;
import com.server.running_handai.course.entity.Theme;
import com.server.running_handai.course.entity.CourseFilter;
import com.server.running_handai.course.service.CourseService;
import com.server.running_handai.global.response.ApiResponse;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseInfoDto>>> getFilteredCourses(
            @RequestParam(value = "filter") CourseFilter filter,
            @RequestParam(value = "lat") Double lat,
            @RequestParam(value = "lon") Double lon,
            @RequestParam(value = "area", required = false) Area area,
            @RequestParam(value = "theme", required = false) Theme theme
            ) {
        log.info("[코스 목록 조회 요청] filter: {}, lat: {}, lon: {}, area: {}, theme: {}", filter, lat, lon, area, theme);
        if (lat == null || lon == null) {
            throw new BusinessException(INVALID_USER_POINT);
        }

        List<CourseInfoDto> courseInfoDtoList = switch (filter) {
            case NEARBY -> // 사용자의 위치 기준 10km 이내의 코스 조회
                    courseService.findCoursesNearby(lat, lon);
            case AREA -> { // 특정 지역의 코스 조회
                if (area == null) {
                    throw new BusinessException(INVALID_AREA_PARAMETER);
                }
                yield courseService.findCoursesByArea(area, lat, lon);
            }
            case THEME -> { // 특정 테마의 코스 조회
                if (theme == null) {
                    throw new BusinessException(INVALID_THEME_PARAMETER);
                }
                yield courseService.findCoursesByTheme(theme, lat, lon);
            }
        };

        if (courseInfoDtoList.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(SUCCESS_EMPTY_COURSE_INFO, courseInfoDtoList));
        }

        return ResponseEntity.ok(ApiResponse.success(SUCCESS, courseInfoDtoList));
    }
}
