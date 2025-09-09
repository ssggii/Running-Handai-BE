package com.server.running_handai.domain.member.dto;

import com.server.running_handai.domain.bookmark.dto.MyBookmarkDetailDto;
import com.server.running_handai.domain.course.dto.MyAllCoursesDetailDto;
import com.server.running_handai.domain.member.entity.Member;

public record MemberInfoDto(
        String nickname,
        String email,
        MyBookmarkDetailDto bookmarkInfo,
        MyAllCoursesDetailDto myCourseInfo
) {
    public static MemberInfoDto from(Member member, MyBookmarkDetailDto bookmarkInfo, MyAllCoursesDetailDto myCourseInfo) {
        return new MemberInfoDto(
                member.getNickname(),
                member.getEmail(),
                bookmarkInfo,
                myCourseInfo
        );
    }
}
