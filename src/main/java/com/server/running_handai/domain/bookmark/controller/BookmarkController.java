package com.server.running_handai.domain.bookmark.controller;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseDetailDto;
import com.server.running_handai.domain.bookmark.service.BookmarkService;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "북마크 관련 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "북마크 등록", description = "특정 코스를 북마크합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)"),
            @ApiResponse(responseCode = "400", description = "실패 (이미 북마크한 코스)"),
            @ApiResponse(responseCode = "401", description = "실패 (인증 실패)")
    })
    @PostMapping("/api/courses/{courseId}/bookmarks")
    public ResponseEntity<CommonResponse<?>> registerBookmark(
            @Parameter(description = "북마크 대상 코스", required = true) @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[북마크 등록] 시작: memberId={}, courseId={}", memberId, courseId);
        bookmarkService.createBookmark(memberId, courseId);
        log.info("[북마크 등록] 성공: memberId={}, courseId={}", memberId, courseId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS_BOOKMARK_CREATE, null));
    }

    @Operation(summary = "북마크 해제", description = "특정 코스를 북마크 해제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 코스)"),
            @ApiResponse(responseCode = "404", description = "실패 (존재하지 않는 북마크)"),
            @ApiResponse(responseCode = "401", description = "실패 (인증 실패)")
    })
    @DeleteMapping("/api/courses/{courseId}/bookmarks")
    public ResponseEntity<CommonResponse<?>> deleteBookmark(
            @Parameter(description = "북마크 대상 코스", required = true) @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[북마크 해제] 시작: memberId={}, courseId={}", memberId, courseId);
        bookmarkService.deleteBookmark(memberId, courseId);
        log.info("[북마크 해제] 성공: memberId={}, courseId={}", memberId, courseId);
        return ResponseEntity.ok(CommonResponse.success(SUCCESS_BOOKMARK_DELETE, null));
    }

    @Operation(summary = "북마크한 코스 조회", description = "회원이 북마크한 코스를 조회합니다. 코스의 지역으로 조건 검색이 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "200", description = "성공 (북마크한 코스 없음)"),
            @ApiResponse(responseCode = "400", description = "실패 (요청 파라미터 오류)"),
            @ApiResponse(responseCode = "401", description = "실패 (인증 실패)")
    })
    @GetMapping("/api/members/me/courses/bookmarks")
    public ResponseEntity<CommonResponse<?>> getBookmarkedCourses(
            @Parameter(description = "페이지 번호", required = true) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지에 조회할 개수", required = true) @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "지역 조건 (전체 보기인 경우 null)") @RequestParam(required = false) Area area,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[북마크한 코스 조회] memberId={}, area={}", memberId, area);
        BookmarkedCourseDetailDto bookmarkedCourseDetailDto =
                bookmarkService.findBookmarkedCourses(memberId, area, PageRequest.of(page, size));
        return ResponseEntity.ok(CommonResponse.success(SUCCESS, bookmarkedCourseDetailDto));
    }
}
