package com.server.running_handai.domain.member.service;

import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static com.server.running_handai.global.response.ResponseCode.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("닉네임 유효성 검증 메서드 테스트")
    class NicknameValidationTest {
        /**
         * [닉네임 유효성 검증 메서드] 실패
         * 1. 현재 자신의 닉네임과 동일한 경우
         */
        @Test
        @DisplayName("닉네임 유효성 검증 메서드 - 본인 닉네임과 동일")
        void isNicknameValid_fail_sameAsCurrentNickname() {
            // given
            String currentNickname = "current";
            String newNickname = "current";

            // when & then
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, currentNickname));
            assertThat(exception.getResponseCode()).isEqualTo(SAME_AS_CURRENT_NICKNAME);
        }

        /**
         * [닉네임 유효성 검증 메서드] 실패
         * 2. 글자수가 안맞는 경우 (2글자 ~ 10글자)
         */
        @ParameterizedTest
        @ValueSource(strings = {"a", "verylongnickname123"})
        @DisplayName("닉네임 유효성 검증 메서드 - 글자수가 안맞음")
        void isNicknameValid_fail_inValidNicknameLength(String newNickname) {
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, "current"));
            assertThat(exception.getResponseCode()).isEqualTo(INVALID_NICKNAME_LENGTH);
        }

        /**
         * [닉네임 유효성 검증 메서드] 실패
         * 3. 한글, 영문, 숫자 외의 문자가 존재하는 경우
         */
        @ParameterizedTest
        @ValueSource(strings = {"hello@", "닉네임!", "test#123"})
        @DisplayName("닉네임 유효성 검증 메서드 - 한글, 영문, 숫자 외의 문자 존재")
        void isNicknameValid_fail_inValidNicknameFormat(String newNickname) {
            BusinessException exception = assertThrows(BusinessException.class, () -> memberService.isNicknameValid(newNickname, "current"));
            assertThat(exception.getResponseCode()).isEqualTo(INVALID_NICKNAME_FORMAT);
        }
    }

}
