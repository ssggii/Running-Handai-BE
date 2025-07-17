package com.server.running_handai.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.server.running_handai.bookmark.entity.Bookmark;
import com.server.running_handai.bookmark.repository.BookmarkRepository;
import com.server.running_handai.course.entity.Course;
import com.server.running_handai.course.repository.CourseRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.member.entity.Member;
import com.server.running_handai.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @InjectMocks
    private BookmarkService bookmarkService;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private Member member;

    @Mock
    private Course course;

    @Mock
    private Bookmark bookmark;

    @Test
    @DisplayName("북마크 등록 성공")
    void registerBookmark_success() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.existsByMemberAndCourse(member, course)).willReturn(false);

        // when
        bookmarkService.registerBookmark(memberId, courseId);

        // then
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 등록 실패 - 이미 북마크한 코스")
    void registerBookmark_fail_alreadyBookmarked() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.existsByMemberAndCourse(member, course)).willReturn(true);

        // when
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.registerBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.ALREADY_BOOKMARKED);
    }

    @Test
    @DisplayName("북마크 등록 실패 - 존재하지 않는 회원")
    void registerBookmark_fail_memberNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.registerBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("북마크 등록 실패 - 존재하지 않는 코스")
    void registerBookmark_fail_courseNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.registerBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @Test
    @DisplayName("북마크 해제 성공")
    void deleteBookmark_success() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.findByMemberAndCourse(member, course)).willReturn(Optional.of(bookmark));

        // when
        bookmarkService.deleteBookmark(memberId, courseId);

        // then
        verify(bookmarkRepository, times(1)).delete(bookmark);
    }

    @Test
    @DisplayName("북마크 해제 실패 - 존재하지 않는 북마크")
    void deleteBookmark_fail_bookmarkNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.findByMemberAndCourse(member, course)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.deleteBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.BOOKMARK_NOT_FOUND);
    }

    @Test
    @DisplayName("북마크 해제 실패 - 존재하지 않는 회원")
    void deleteBookmark_fail_memberNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.deleteBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("북마크 해제 실패 - 존재하지 않는 코스")
    void deleteBookmark_fail_courseNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.deleteBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }
}