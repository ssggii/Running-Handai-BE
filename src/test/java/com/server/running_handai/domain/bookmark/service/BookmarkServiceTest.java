package com.server.running_handai.domain.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
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
    void createBookmark_success() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId)).willReturn(false);

        // when
        bookmarkService.createBookmark(memberId, courseId);

        // then
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 등록 실패 - 이미 북마크한 코스")
    void createBookmark_fail_alreadyBookmarked() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(bookmarkRepository.existsByCourseIdAndMemberId(courseId, memberId)).willReturn(true);

        // when
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.createBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.ALREADY_BOOKMARKED);
    }

    @Test
    @DisplayName("북마크 등록 실패 - 존재하지 않는 회원")
    void createBookmark_fail_memberNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.createBookmark(memberId, courseId));

        // then
        assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("북마크 등록 실패 - 존재하지 않는 코스")
    void createBookmark_fail_courseNotFound() {
        // given
        Long memberId = 1L;
        Long courseId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // when, then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookmarkService.createBookmark(memberId, courseId));

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

    @Nested
    @DisplayName("북마크한 코스 조회 테스트")
    class GetBookmarkedCourseTest {

        private static Stream<Arguments> provideAreaArguments() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(Area.HAEUN_GWANGAN)
            );
        }

        @ParameterizedTest
        @MethodSource("provideAreaArguments")
        @DisplayName("북마크한 코스 조회 성공 - 결과 있음")
        void getBookmarkedCourse_success_hasContent(Area area) {
            // given
            Long memberId = 1L;
            BookmarkedCourseInfoDto dto1 = mock(BookmarkedCourseInfoDto.class);
            BookmarkedCourseInfoDto dto2 = mock(BookmarkedCourseInfoDto.class);
            List<BookmarkedCourseInfoDto> expectedDtos = List.of(dto1, dto2);

            if (area == null) {
                given(bookmarkRepository.findBookmarkedCoursesByMemberId(memberId)).willReturn(expectedDtos);
            } else {
                given(bookmarkRepository.findBookmarkedCoursesByMemberIdAndArea(memberId, area)).willReturn(expectedDtos);
            }

            // when
            List<BookmarkedCourseInfoDto> actualDtos =
                    bookmarkService.findBookmarkedCourses(memberId, area);

            // then
            assertThat(actualDtos.size()).isEqualTo(expectedDtos.size());
            assertThat(actualDtos).isEqualTo(expectedDtos);

            if (area == null) {
                verify(bookmarkRepository).findBookmarkedCoursesByMemberId(memberId);
                verify(bookmarkRepository, never()).findBookmarkedCoursesByMemberIdAndArea(any(), any());
            } else {
                verify(bookmarkRepository, never()).findBookmarkedCoursesByMemberId(any());
                verify(bookmarkRepository).findBookmarkedCoursesByMemberIdAndArea(memberId, area);
            }
        }

        @ParameterizedTest
        @MethodSource("provideAreaArguments")
        @DisplayName("북마크한 코스 조회 성공 - 결과 없음")
        void getBookmarkedCourse_success_noContent(Area area) {
            // given
            Long memberId = 1L;

            if (area == null) {
                given(bookmarkRepository.findBookmarkedCoursesByMemberId(memberId)).willReturn(Collections.emptyList());
            } else {
                given(bookmarkRepository.findBookmarkedCoursesByMemberIdAndArea(memberId, area)).willReturn(Collections.emptyList());
            }

            // when
            List<BookmarkedCourseInfoDto> result =
                    bookmarkService.findBookmarkedCourses(memberId, area);

            // then
            assertThat(result).isEmpty();

            if (area == null) {
                verify(bookmarkRepository).findBookmarkedCoursesByMemberId(memberId);
                verify(bookmarkRepository, never()).findBookmarkedCoursesByMemberIdAndArea(any(), any());
            } else {
                verify(bookmarkRepository, never()).findBookmarkedCoursesByMemberId(any());
                verify(bookmarkRepository).findBookmarkedCoursesByMemberIdAndArea(memberId, area);
            }
        }
    }
}