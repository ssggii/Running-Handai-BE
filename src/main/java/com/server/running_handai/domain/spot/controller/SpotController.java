package com.server.running_handai.domain.spot.controller;

import com.server.running_handai.domain.spot.dto.SpotDetailDto;
import com.server.running_handai.domain.spot.service.SpotService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.server.running_handai.domain.course.entity.SpotStatus.IN_PROGRESS;
import static com.server.running_handai.global.response.ResponseCode.SUCCESS;
import static com.server.running_handai.global.response.ResponseCode.SUCCESS_SPOT_INIT_IN_PROGRESS;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Spot", description = "즐길거리 관련 API")
public class SpotController {

    private final SpotService spotService;

    @Operation(summary = "즐길거리 전체 조회", description = "특정 코스의 즐길거리 전체 정보를 조회합니다."
            + "<br> 즐길거리는 초기화 상태(spotStatus)에 따라 다르게 반환합니다.<br>"
            + "<br> 초기화 완료(COMPLETED) - 즐길거리 리스트 반환 (조회 결과 없을 수 있음)"
            + "<br> 진행 중(IN_PROGRESS), 초기화 실패(FAILED), 해당없음(NOT_APPLICABLE) - 빈 리스트"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)"),
    })
    @GetMapping("/{courseId}/spots")
    public ResponseEntity<CommonResponse<SpotDetailDto>> getSpotDetails(
            @Parameter(description = "조회하려는 코스 ID", required = true) @PathVariable("courseId") Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        log.info("[즐길거리 전체 조회] courseId: {}, memberId: {}", courseId, memberId);
        SpotDetailDto spotDetailDto = spotService.getSpotDetails(courseId);
        String status = spotDetailDto.spotStatus();

        // 즐길거리 초기화 진행 중일 때 202 accepted 반환
        if (status.equals(IN_PROGRESS.name())) {
            return ResponseEntity.accepted()
                    .body(CommonResponse.success(SUCCESS_SPOT_INIT_IN_PROGRESS, spotDetailDto));
        }

        return ResponseEntity.ok(CommonResponse.success(SUCCESS, spotDetailDto));
    }
}
