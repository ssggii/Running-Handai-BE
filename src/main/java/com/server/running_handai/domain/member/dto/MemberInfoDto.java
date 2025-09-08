package com.server.running_handai.domain.member.dto;

import com.server.running_handai.domain.bookmark.dto.MyBookmarkDetailDto;
import com.server.running_handai.domain.course.dto.MyAllCoursesDetailDto;
import com.server.running_handai.domain.member.entity.Member;

public record MemberInfoDto(
        String nickname,
        String email,
        MyBookmarkDetailDto bookmarkedCourses,
        MyAllCoursesDetailDto myCourses
) {
    public static MemberInfoDto from(Member member, MyBookmarkDetailDto bookmarkedCourses, MyAllCoursesDetailDto myCourses) {
        return new MemberInfoDto(
                member.getNickname(),
                member.getEmail(),
                bookmarkedCourses,
                myCourses
        );
    }
}
