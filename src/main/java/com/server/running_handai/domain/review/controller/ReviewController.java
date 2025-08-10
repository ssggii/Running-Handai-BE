package com.server.running_handai.domain.review.controller;

import com.server.running_handai.domain.review.dto.MyReviewInfoDto;
import com.server.running_handai.domain.review.dto.ReviewCreateResponseDto;
import com.server.running_handai.domain.review.dto.ReviewInfoListDto;
import com.server.running_handai.domain.review.dto.ReviewCreateRequestDto;
import com.server.running_handai.domain.review.dto.ReviewUpdateRequestDto;
import com.server.running_handai.domain.review.dto.ReviewUpdateResponseDto;
import com.server.running_handai.domain.review.service.ReviewService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Review", description = "리뷰 관련 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 등록", description = "코스의 리뷰를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패 (요청 파라미터 오류)"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)")
    })
    @PostMapping("/api/courses/{courseId}/reviews")
    public ResponseEntity<CommonResponse<ReviewCreateResponseDto>> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        ReviewCreateResponseDto responseData = reviewService.createReview(courseId, requestDto, memberId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, responseData));
    }

    @Operation(summary = "리뷰 조회", description = "코스의 리뷰를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "200", description = "성공 (리뷰 없음)"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)")
    })
    @GetMapping("/api/courses/{courseId}/reviews")
    public ResponseEntity<CommonResponse<ReviewInfoListDto>> getReviewsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = (customOAuth2User != null) ? customOAuth2User.getMember().getId() : null;
        ReviewInfoListDto responseData = reviewService.findAllReviewsByCourse(courseId, memberId);

        if (responseData.reviewCount() == 0) {
            return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, responseData));
        }
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, responseData));
    }

    @Operation(summary = "리뷰 수정", description = "코스의 리뷰를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패 (요청 파라미터 오류)"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 리뷰)"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @PatchMapping("/api/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<ReviewUpdateResponseDto>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        ReviewUpdateResponseDto responseData = reviewService.updateReview(reviewId, requestDto, customOAuth2User.getMember());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, responseData));
    }

    @Operation(summary = "리뷰 삭제", description = "코스의 리뷰를 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 리뷰)"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음")
    })
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<CommonResponse<?>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        reviewService.deleteReview(reviewId, customOAuth2User.getMember().getId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, null));
    }

    @Operation(summary = "내 리뷰 조회", description = "회원이 작성한 리뷰를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "200", description = "성공 (리뷰 없음)")
    })
    @GetMapping("/api/me/reviews")
    public ResponseEntity<CommonResponse<List<MyReviewInfoDto>>> getMyReviews(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        List<MyReviewInfoDto> responseData = reviewService.getMyReviews(customOAuth2User.getMember().getId());

        if (responseData.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS_EMPTY_REVIEWS, responseData));
        }

        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, responseData));
    }

}
