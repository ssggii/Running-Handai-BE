package com.server.running_handai.domain.member.service;

import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseDetailDto;
import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseInfoDto;
import com.server.running_handai.domain.bookmark.dto.MyBookmarkDetailDto;
import com.server.running_handai.domain.bookmark.repository.BookmarkRepository;
import com.server.running_handai.domain.bookmark.service.BookmarkService;
import com.server.running_handai.domain.course.dto.MyAllCoursesDetailDto;
import com.server.running_handai.domain.course.dto.MyCourseInfoDto;
import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.course.service.CourseService;
import com.server.running_handai.domain.member.dto.MemberInfoDto;
import com.server.running_handai.domain.member.dto.MemberUpdateRequestDto;
import com.server.running_handai.domain.member.dto.MemberUpdateResponseDto;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.entity.SortBy;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.server.running_handai.global.response.ResponseCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkService bookmarkService;

    @Mock
    private CourseService courseService;

    private Member createMockMember(Long memberId) {
        Member member = Member.builder().nickname("current").email("email").build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    @Nested
    @DisplayName("닉네임 유효성 검증 테스트")
    class NicknameValidationTest {

        /**
         * [닉네임 유효성 검증] 실패
         * 1. 현재 자신의 닉네임과 동일한 경우
         */
        @Test
        @DisplayName("닉네임 유효성 검증 실패 - 본인 닉네임과 동일")
        void isNicknameValid_fail_sameAsCurrentNickname() {
            // given
            String currentNickname = "current";
            String newNickname = "current";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, currentNickname));
            assertThat(exception.getResponseCode()).isEqualTo(SAME_AS_CURRENT_NICKNAME);
        }

        /**
         * [닉네임 유효성 검증] 실패
         * 2. 글자수가 안맞는 경우 (2글자 ~ 10글자)
         */
        @ParameterizedTest
        @ValueSource(strings = {"a", "verylongnickname123"})
        @DisplayName("닉네임 유효성 검증 실패 - 글자수가 안맞음")
        void isNicknameValid_fail_inValidNicknameLength(String newNickname) {
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, "current"));
            assertThat(exception.getResponseCode()).isEqualTo(INVALID_NICKNAME_LENGTH);
        }

        /**
         * [닉네임 유효성 검증] 실패
         * 3. 한글, 영문, 숫자 외의 문자가 존재하는 경우
         */
        @ParameterizedTest
        @ValueSource(strings = {"hello@", "닉네임!", "test#123"})
        @DisplayName("닉네임 유효성 검증 실패 - 한글, 영문, 숫자 외의 문자 존재")
        void isNicknameValid_fail_inValidNicknameFormat(String newNickname) {
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, "current"));
            assertThat(exception.getResponseCode()).isEqualTo(INVALID_NICKNAME_FORMAT);
        }
    }

    @Nested
    @DisplayName("닉네임 중복 여부 조회 테스트")
    class CheckNicknameDuplicateTest {
        private static final Long MEMBER_ID = 1L;

        /**
         * [닉네임 중복 여부 조회] 성공
         * 1. 중복되지 않은 닉네임인 경우 (true 응답)
         */
        @Test
        @DisplayName("닉네임 중복 확인 성공 - 중복되지 않은 닉네임")
        void checkNicknameDuplicate_success_notDuplicateNickname() {
            // given
            Member member = createMockMember(MEMBER_ID);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            String newNickname = "new";
            given(memberRepository.existsByNickname("new")).willReturn(false);

            // when
            Boolean result = memberService.checkNicknameDuplicate(member.getId(), newNickname);

            // then
            assertThat(result).isTrue();
            verify(memberRepository).findById(MEMBER_ID);
            verify(memberRepository).existsByNickname(newNickname);
        }

        /**
         * [닉네임 중복 여부 조회] 성공
         * 2. 중복된 닉네임인 경우 (false 응답)
         */
        @Test
        @DisplayName("닉네임 중복 확인 성공 - 중복된 닉네임")
        void checkNicknameDuplicate_success_duplicateNickname() {
            // given
            Member member = createMockMember(MEMBER_ID);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            String newNickname = "duplicate";
            given(memberRepository.existsByNickname("duplicate")).willReturn(true);

            // when
            Boolean result = memberService.checkNicknameDuplicate(member.getId(), newNickname);

            // then
            assertThat(result).isFalse();
            verify(memberRepository).findById(MEMBER_ID);
            verify(memberRepository).existsByNickname(newNickname);
        }

        /**
         * [닉네임 중복 여부 조회] 실패
         * 1. Member가 존재하지 않을 경우
         */
        @Test
        @DisplayName("닉네임 중복 확인 실패 - 찾을 수 없는 사용자")
        void checkNicknameDuplicate_fail_memberNotFound() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when, then
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.checkNicknameDuplicate(MEMBER_ID, anyString()));
            assertThat(exception.getResponseCode()).isEqualTo(MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내 정보 수정 테스트")
    class UpdateMemberInfoTest {
        private static final Long MEMBER_ID = 1L;

        /**
         * [내 정보 수정] 성공
         * 1. 수정을 성공한 경우
         */
        @Test
        @DisplayName("내 정보 수정 성공")
        void updateMemberInfo_success() {
            // given
            Member member = createMockMember(MEMBER_ID);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            MemberUpdateRequestDto memberUpdateRequestDto = new MemberUpdateRequestDto("new");
            given(memberRepository.existsByNickname("new")).willReturn(false);

            // when
            MemberUpdateResponseDto memberUpdateResponseDto = memberService.updateMemberInfo(member.getId(), memberUpdateRequestDto);

            // then
            assertThat(memberUpdateResponseDto.nickname()).isEqualTo("new");
            verify(memberRepository).findById(MEMBER_ID);
            verify(memberRepository).existsByNickname("new");
        }

        /**
         * [내 정보 수정] 실패
         * 1. Member가 존재하지 않을 경우
         */
        @Test
        @DisplayName("내 정보 수정 실패 - 찾을 수 없는 사용자")
        void updateMemberInfo_fail_memberNotFound() {
            // given
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());
            MemberUpdateRequestDto memberUpdateRequestDto = new MemberUpdateRequestDto("new");

            // when, then
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.updateMemberInfo(MEMBER_ID, memberUpdateRequestDto));
            assertThat(exception.getResponseCode()).isEqualTo(MEMBER_NOT_FOUND);
        }

        /**
         * [내 정보 수정] 실패
         * 2. 중복된 닉네임인 경우
         */
        @Test
        @DisplayName("내 정보 수정 - 중복된 닉네임")
        void updateMemberInfo_fail_duplicateNickname() {
            // given
            Member member = createMockMember(MEMBER_ID);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

            MemberUpdateRequestDto memberUpdateRequestDto = new MemberUpdateRequestDto("duplicate");
            given(memberRepository.existsByNickname("duplicate")).willReturn(true);

            // when, then
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.updateMemberInfo(MEMBER_ID, memberUpdateRequestDto));
            assertThat(exception.getResponseCode()).isEqualTo(DUPLICATE_NICKNAME);
        }
    }

    @Nested
    @DisplayName("내 정보 조회 테스트")
    class GetMemberInfoTest {
        private static final int BOOKMARK_PREVIEW_MAX_COUNT = 5;
        private static final int MY_COURSE_PREVIEW_MAX_COUNT = 3;
        private Pageable pageable = PageRequest.of(0, 10);

        @ParameterizedTest(name = "북마크 {0}개, 내 코스 {1}개일 때")
        @DisplayName("내 정보 조회 - 성공")
        @CsvSource({
                "10, 5", // Case 1: 둘 다 최대 개수보다 많을 때
                "3, 2",  // Case 2: 둘 다 최대 개수보다 적을 때
                "10, 0", // Case 3: 내 코스가 없을 때
                "0, 5",  // Case 4: 북마크가 없을 때
                "0, 0"   // Case 5: 둘 다 없을 때
        })
        void getMemberInfo_success(int bookmarkCount, int myCourseCount) {
            // given
            Long memberId = 1L;
            Member member = mock(Member.class);
            when(member.getNickname()).thenReturn("testUser");
            when(member.getEmail()).thenReturn("test@example.com");
            when(member.getCourses()).thenReturn(Collections.nCopies(myCourseCount, mock(Course.class)));
            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

            Pageable bookmarkPageable = PageRequest.of(0, BOOKMARK_PREVIEW_MAX_COUNT);
            Pageable myCoursePageable = PageRequest.of(0, MY_COURSE_PREVIEW_MAX_COUNT, SortBy.findBySort("LATEST"));

            // 북마크 정보 Mocking
            List<BookmarkedCourseInfoDto> previewBookmarks = IntStream.range(0, Math.min(bookmarkCount, BOOKMARK_PREVIEW_MAX_COUNT))
                    .mapToObj(i -> mock(BookmarkedCourseInfoDto.class))
                    .toList();
            Page<BookmarkedCourseInfoDto> bookmarkPage = new PageImpl<>(previewBookmarks, bookmarkPageable, bookmarkCount);
            BookmarkedCourseDetailDto mockBookmarkInfo = BookmarkedCourseDetailDto.from(bookmarkPage);
            when(bookmarkService.findBookmarkedCourses(eq(memberId), isNull(), eq(bookmarkPageable)))
                    .thenReturn(mockBookmarkInfo);

            // 내 코스 정보 Mocking
            List<MyCourseInfoDto> previewMyCourses = IntStream.range(0, Math.min(myCourseCount, MY_COURSE_PREVIEW_MAX_COUNT))
                    .mapToObj(i -> mock(MyCourseInfoDto.class))
                    .toList();
            MyAllCoursesDetailDto mockMyCourseInfo = MyAllCoursesDetailDto.from(myCourseCount, previewMyCourses);
            when(courseService.getMyAllCourses(eq(memberId), eq(myCoursePageable), isNull()))
                    .thenReturn(mockMyCourseInfo);

            // when
            MemberInfoDto result = memberService.getMemberInfo(memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.nickname()).isEqualTo("testUser");
            assertThat(result.email()).isEqualTo("test@example.com");

            // 전체 개수 검증
            assertThat(result.bookmarkInfo().bookmarkCount()).isEqualTo(bookmarkCount);
            assertThat(result.myCourseInfo().myCourseCount()).isEqualTo(myCourseCount);

            // 미리보기 개수 검증
            assertThat(result.bookmarkInfo().courses().size()).isEqualTo(previewBookmarks.size());
            assertThat(result.myCourseInfo().courses().size()).isEqualTo(previewMyCourses.size());

            verify(memberRepository).findById(memberId);
            verify(bookmarkService).findBookmarkedCourses(eq(memberId), isNull(), eq(bookmarkPageable));
            verify(courseService).getMyAllCourses(eq(memberId), eq(myCoursePageable), isNull());

        }

        @Test
        @DisplayName("내 정보 조회 실패 - 존재하지 않는 회원")
        void getMemberInfo_fail_memberNotFound() {
            // given
            Long nonExistentMemberId = 999L;

            when(memberRepository.findById(nonExistentMemberId)).thenReturn(Optional.empty());

            // when, then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> memberService.getMemberInfo(nonExistentMemberId));

            assertThat(exception.getResponseCode()).isEqualTo(ResponseCode.MEMBER_NOT_FOUND);

            verify(bookmarkService, never()).findBookmarkedCourses(any(), any(), any());
            verify(courseService, never()).getMyAllCourses(any(), any(), any());
        }

    }
}
