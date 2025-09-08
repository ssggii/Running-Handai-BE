package com.server.running_handai.domain.member.service;

import com.server.running_handai.domain.bookmark.dto.BookmarkedCourseInfoDto;
import com.server.running_handai.domain.bookmark.service.BookmarkService;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.dto.MyAllCoursesDetailDto;
import com.server.running_handai.domain.course.dto.MyCourseInfoDto;
import com.server.running_handai.domain.course.service.CourseService;
import com.server.running_handai.domain.member.dto.MemberInfoDto;
import com.server.running_handai.domain.member.dto.MemberUpdateRequestDto;
import com.server.running_handai.domain.member.dto.MemberUpdateResponseDto;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.entity.SortBy;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.server.running_handai.global.response.ResponseCode.*;
import static org.mockito.ArgumentMatchers.any;
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
        private final int MY_COURSE_PREVIEW_MAX_COUNT = 3;

        @ParameterizedTest(name = "북마크 {0}개, 내 코스 {1}개일 때")
        @DisplayName("내 정보 조회 - 성공")
        @CsvSource({
                "10, 3, 5, 3", // Case 1: 북마크 10개, 내 코스 3개
                "0, 3, 0, 3",  // Case 2: 북마크 0개, 내 코스 3개
                "10, 0, 5, 0", // Case 3: 북마크 10개, 내 코스 0개
                "0, 0, 0, 0"   // Case 4: 둘 다 없는 경우
        })
        void getMemberInfo_success_scenarios(int bookmarkCount, int myCourseCount, int expectedBookmarkCount, int expectedMyCourseCount) {
            // given
            Long memberId = 1L;
            Member member = createMockMember(memberId);

            // 북마크한 코스 데이터 생성
            List<BookmarkedCourseInfoDto> mockBookmarkedCourses = IntStream.range(0, bookmarkCount)
                    .mapToObj(i -> mock(BookmarkedCourseInfoDto.class))
                    .toList();

            // 내 코스 데이터 생성
            List<MyCourseInfoDto> mockMyCourses = IntStream.range(0, myCourseCount)
                    .mapToObj(i -> mock(MyCourseInfoDto.class))
                    .toList();
            MyAllCoursesDetailDto myAllCoursesDetailDto = MyAllCoursesDetailDto.from(mockMyCourses);
            Pageable pageable = PageRequest.of(0, MY_COURSE_PREVIEW_MAX_COUNT, SortBy.findBySort("LATEST"));

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
            when(bookmarkService.findBookmarkedCourses(memberId, null)).thenReturn(mockBookmarkedCourses);
            when(courseService.getMyAllCourses(memberId, pageable, null)).thenReturn(myAllCoursesDetailDto);

            // when
            MemberInfoDto result = memberService.getMemberInfo(memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.nickname()).isEqualTo(member.getNickname());
            assertThat(result.email()).isEqualTo(member.getEmail());

            // limit 개수 검증
            assertThat(result.bookmarkedCourses().courseCount()).isEqualTo(expectedBookmarkCount);
            assertThat(result.myCourses().courseCount()).isEqualTo(expectedMyCourseCount);

            verify(memberRepository).findById(memberId);
            verify(bookmarkService).findBookmarkedCourses(memberId, null);
            verify(courseService).getMyAllCourses(memberId, pageable, null);
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

            verify(bookmarkService, never()).findBookmarkedCourses(any(), any());
            verify(courseService, never()).getMyAllCourses(any(), any(), any());
        }

    }
}
