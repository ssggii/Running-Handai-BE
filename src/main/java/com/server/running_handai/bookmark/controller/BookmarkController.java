package com.server.running_handai.bookmark.controller;

import com.server.running_handai.bookmark.service.BookmarkService;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        bookmarkService.registerBookmark(customOAuth2User.getMember().getId(), courseId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS_BOOKMARK_CREATE, null));
    }

    @DeleteMapping
    public ResponseEntity<CommonResponse<?>> deleteBookmark(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        bookmarkService.deleteBookmark(customOAuth2User.getMember().getId(), courseId);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS_BOOKMARK_DELETE, null));
    }

}
