package com.server.running_handai.domain.spot.controller;

import com.server.running_handai.domain.spot.dto.SpotDetailDto;
import com.server.running_handai.domain.spot.service.SpotService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.server.running_handai.global.response.ResponseCode.SUCCESS;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class SpotController {
    private final SpotService spotService;

    @Operation(summary = "즐길거리 전체 조회", description = "특정 코스의 즐길거리 전체 정보를 조회합니다.")
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
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, spotDetailDto));
    }
}
