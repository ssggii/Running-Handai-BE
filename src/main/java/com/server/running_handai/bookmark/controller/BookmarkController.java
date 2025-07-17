package com.server.running_handai.bookmark.controller;

import com.server.running_handai.bookmark.service.BookmarkService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<CommonResponse<?>> registerBookmark(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[북마크 등록] 시작: memberId={}, courseId={}", memberId, courseId);
        bookmarkService.registerBookmark(memberId, courseId);
        log.info("[북마크 등록] 성공: memberId={}, courseId={}", memberId, courseId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS_BOOKMARK_CREATE, null));
    }

    @DeleteMapping
    public ResponseEntity<CommonResponse<?>> deleteBookmark(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[북마크 해제] 시작: memberId={}, courseId={}", memberId, courseId);
        bookmarkService.deleteBookmark(memberId, courseId);
        log.info("[북마크 해제] 성공: memberId={}, courseId={}", memberId, courseId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS_BOOKMARK_DELETE, null));
    }

}
