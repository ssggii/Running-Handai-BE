package com.server.running_handai.domain.course.controller;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.dto.CourseWithPointDto;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.domain.course.entity.CourseFilter;
import com.server.running_handai.domain.course.service.CourseService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<CommonResponse<List<CourseWithPointDto>>> getFilteredCourses(
            @Parameter(description = "코스 필터링 옵션", required = true)
            @RequestParam(value = "filter") CourseFilter filter,

            @Parameter(description = "사용자 현위치의 위도", required = true, example = "35.10")
            @RequestParam(value = "lat") Double lat,

            @Parameter(description = "사용자 현위치의 경도", required = true, example = "129.12")
            @RequestParam(value = "lon") Double lon,

            @Parameter(description = "지역 필터링 시 사용할 지역 코드")
            @RequestParam(value = "area", required = false) Area area,

            @Parameter(description = "테마 필터링 시 사용할 테마 코드")
            @RequestParam(value = "theme", required = false) Theme theme,

            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {

        log.info("[코스 목록 조회 요청] filter: {}, lat: {}, lon: {}, area: {}, theme: {}", filter, lat, lon, area, theme);
        if (lat == null || lon == null) {
            throw new BusinessException(INVALID_USER_POINT);
        }

        // 회원인 경우 memberId를, 비회원인 경우 null을 서비스로 전달
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;

        List<CourseWithPointDto> responseData = switch (filter) {
            case NEARBY -> // 사용자의 위치 기준 10km 이내의 코스 조회
                    courseService.findCoursesNearby(lat, lon, memberId);
            case AREA -> { // 특정 지역의 코스 조회
                if (area == null) {
                    throw new BusinessException(INVALID_AREA_PARAMETER);
                }
                yield courseService.findCoursesByArea(area, lat, lon, memberId);
            }
            case THEME -> { // 특정 테마의 코스 조회
                if (theme == null) {
                    throw new BusinessException(INVALID_THEME_PARAMETER);
                }
                yield courseService.findCoursesByTheme(theme, lat, lon, memberId);
            }
        };

        if (responseData.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success(SUCCESS_EMPTY_COURSE_INFO, responseData));
        }

        return ResponseEntity.ok(CommonResponse.success(SUCCESS, responseData));
    }

    @Operation(summary = "추천코스 상세 조회", description = "특정 추천코스의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)"),
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<CommonResponse<CourseDetailDto>> getCourseDetails(
            @Parameter(description = "조회하려는 코스 ID", required = true)
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        log.info("[코스 상세정보 조회 요청] courseId: {}", courseId);
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        CourseDetailDto courseDetails = courseService.findCourseDetails(courseId, memberId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseDetails));
    }

}
