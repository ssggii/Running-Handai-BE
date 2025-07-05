package com.server.running_handai.course.controller;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.course.dto.CourseDetailDto;
import com.server.running_handai.course.dto.CourseInfoDto;
import com.server.running_handai.course.entity.Area;
import com.server.running_handai.course.entity.Theme;
import com.server.running_handai.course.entity.CourseFilter;
import com.server.running_handai.course.service.CourseService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "코스 관련 API")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "추천코스 전체 조회", description = "추천코스를 다양한 필터 옵션으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패 (요청 파라미터 오류)"),
    })
    @GetMapping
    public ResponseEntity<CommonResponse<List<CourseInfoDto>>> getFilteredCourses(
            @Parameter(description = "코스 필터링 옵션", required = true)
            @RequestParam(value = "filter") CourseFilter filter,

            @Parameter(description = "사용자 현위치의 위도", required = true, example = "35.10")
            @RequestParam(value = "lat") Double lat,

            @Parameter(description = "사용자 현위치의 경도", required = true, example = "129.12")
            @RequestParam(value = "lon") Double lon,

            @Parameter(description = "지역 필터링 시 사용할 지역 코드")
            @RequestParam(value = "area", required = false) Area area,

            @Parameter(description = "테마 필터링 시 사용할 테마 코드")
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
            return ResponseEntity.ok(CommonResponse.success(SUCCESS_EMPTY_COURSE_INFO, courseInfoDtoList));
        }

        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseInfoDtoList));
    }

    @Operation(summary = "추천코스 상세 조회", description = "특정 추천코스의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)"),
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<CommonResponse<CourseDetailDto>> getCourseDetails(
            @Parameter(description = "조회하려는 코스 ID", required = true)
            @PathVariable("courseId") Long courseId) {
        log.info("[코스 상세정보 조회 요청] courseId: {}", courseId);
        CourseDetailDto courseDetails = courseService.findCourseDetails(courseId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseDetails));
    }

}
