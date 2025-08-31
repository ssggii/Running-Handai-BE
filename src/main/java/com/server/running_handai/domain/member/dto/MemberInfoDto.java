package com.server.running_handai.domain.member.dto;

import com.server.running_handai.domain.bookmark.dto.MyBookmarkInfoDto;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.member.entity.Member;
import java.util.List;

public record MemberInfoDto(
        String nickname,
        String email,
        int BookmarkTotalCount,
        List<MyBookmarkInfoDto> bookmarkedCourses,
        int MyCourseTotalCount,
        List<CourseInfoDto> myCourses
        ) {
    public static MemberInfoDto from(Member member, List<MyBookmarkInfoDto> bookmarkedCourses, List<CourseInfoDto> myCourses) {
        return new MemberInfoDto(
                member.getNickname(),
                member.getEmail(),
                bookmarkedCourses.size(),
                bookmarkedCourses,
                myCourses.size(),
                myCourses
        );
    }
}
