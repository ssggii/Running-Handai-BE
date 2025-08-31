package com.server.running_handai.domain.member.dto;

import com.server.running_handai.domain.bookmark.dto.MyBookmarkInfoDto;
import com.server.running_handai.domain.member.entity.Member;
import java.util.List;

public record MemberInfoDto(
        String nickname,
        String email,
        int totalBookmarks,
        List<MyBookmarkInfoDto> bookmarkedCourses
) {
    public static MemberInfoDto from(Member member, List<MyBookmarkInfoDto> bookmarkedCourses) {
        return new MemberInfoDto(
                member.getNickname(),
                member.getEmail(),
                bookmarkedCourses.size(),
                bookmarkedCourses
        );
    }
}
