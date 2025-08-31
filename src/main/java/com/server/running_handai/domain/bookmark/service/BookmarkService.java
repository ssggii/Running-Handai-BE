package com.server.running_handai.domain.bookmark.service;

import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseInfoDto;
import com.server.running_handai.domain.bookmark.entity.Bookmark;
import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.repository.CourseRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public void createBookmark(Long memberId, Long courseId) {
        // 회원과 코스 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        // 이미 북마크한 코스인지 확인
        if (bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId)) {
            throw new BusinessException(ResponseCode.ALREADY_BOOKMARKED);
        }

        // 북마크 생성 및 저장
        Bookmark bookmark = Bookmark.builder().member(member).course(course).build();
        bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void deleteBookmark(Long memberId, Long courseId) {
        // 회원과 코스 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(ResponseCode.COURSE_NOT_FOUND));

        // 삭제할 북마크 조회
        Bookmark bookmark = bookmarkRepository.findByMemberAndCourse(member, course)
                .orElseThrow(() -> new BusinessException(ResponseCode.BOOKMARK_NOT_FOUND));

        // 북마크 삭제
        bookmarkRepository.delete(bookmark);
    }

    /**
     * 회원이 북마크한 코스를 조회합니다.
     *
     * @param memberId 요청한 회원의 ID
     * @return 북마크한 코스 정보 DTO
     */
    public List<BookmarkedCourseInfoDto> findBookmarkedCourses(Long memberId, Area area) {
        List<BookmarkedCourseInfoDto> bookmarkedCourseInfoDtos;
        if (area == null) { // 지역 전체인 경우
            bookmarkedCourseInfoDtos = bookmarkRepository.findBookmarkedCoursesByMemberId(memberId);
        } else { // 특정 지역 필터링한 경우
            bookmarkedCourseInfoDtos = bookmarkRepository.findBookmarkedCoursesByMemberIdAndArea(memberId, area);
        }

        if (bookmarkedCourseInfoDtos.isEmpty()) {
            return Collections.emptyList();
        }

        return bookmarkedCourseInfoDtos;
    }
}
