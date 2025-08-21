package com.server.running_handai.domain.course.controller;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.domain.course.dto.CourseDetailDto;
import com.server.running_handai.domain.course.dto.CourseFilterRequestDto;
import com.server.running_handai.domain.course.dto.CourseInfoWithDetailsDto;
import com.server.running_handai.domain.course.dto.CourseSummaryDto;
import com.server.running_handai.domain.course.dto.GpxCourseRequestDto;
import com.server.running_handai.domain.course.service.CourseService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Course", description = "코스 관련 API")
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "추천코스 전체 조회", description = "추천코스를 다양한 필터 옵션으로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패 (요청 파라미터 오류)")
    })
    @GetMapping("/api/courses")
    public ResponseEntity<CommonResponse<List<CourseInfoWithDetailsDto>>> getFilteredCourses(
            @ParameterObject @ModelAttribute CourseFilterRequestDto filterOption,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        log.info("[코스 전체 조회] Filter: {}, Member ID: {}", filterOption, memberId);
        List<CourseInfoWithDetailsDto> responseData = courseService.findCourses(filterOption, memberId);

        if (responseData.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success(SUCCESS_EMPTY_COURSE_INFO, responseData));
        }
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, responseData));
    }

    @Operation(summary = "추천코스 상세 조회", description = "특정 추천코스의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)")
    })
    @GetMapping("/api/courses/{courseId}")
    public ResponseEntity<CommonResponse<CourseDetailDto>> getCourseDetails(
            @Parameter(description = "조회하려는 코스 ID", required = true)
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        log.info("[코스 상세 조회] courseId: {}, memberId: {}", courseId, memberId);
        CourseDetailDto courseDetails = courseService.findCourseDetails(courseId, memberId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseDetails));
    }

    @Operation(summary = "추천코스 요약 조회", description = "코스의 요약 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)")
    })
    @GetMapping("/api/courses/{courseId}/summary")
    public ResponseEntity<CommonResponse<CourseSummaryDto>> getCourseSummary(
            @Parameter(description = "조회하려는 코스 ID", required = true)
            @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        log.info("[코스 요약 조회] courseId: {}, memberId: {}", courseId, memberId);
        CourseSummaryDto courseSummary = courseService.getCourseSummary(courseId, memberId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseSummary));
    }

    @Operation(summary = "내 코스 생성", description = "회원의 코스를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 오류")
    })
    @PostMapping(value = "/api/members/me/courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Long>> createMemberCourseWithGpx(
            @RequestPart("pointNames") GpxCourseRequestDto pointNames,
            @RequestPart("gpxFile") MultipartFile gpxFile,
            @RequestPart("thumbnailImage") MultipartFile thumbnailImageFile,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("startPointName: {}, endPointName: {}", pointNames.startPointName(), pointNames.endPointName());
        Long courseId = courseService.createMemberCourse(memberId, pointNames, gpxFile, thumbnailImageFile);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, courseId));
    }

}
