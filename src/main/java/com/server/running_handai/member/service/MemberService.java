package com.server.running_handai.member.service;

import com.server.running_handai.global.jwt.JwtProvider;
import com.server.running_handai.global.oauth.userInfo.OAuth2UserInfo;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.member.dto.TokenRequest;
import com.server.running_handai.member.dto.TokenResponse;
import com.server.running_handai.member.entity.Member;
import com.server.running_handai.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.server.running_handai.global.response.ResponseCode.INVALID_REFRESH_TOKEN;
import static com.server.running_handai.global.response.ResponseCode.REFRESH_TOKEN_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    /**
     * OAuth2 사용자 정보를 기반으로 회원을 생성하거나 기존 회원을 조회합니다.
     *
     * @param oAuth2UserInfo OAuth2 Provider에게 받은 사용자 정보
     * @return 생성되거나 조회된 Member 엔티티
     */
    public Member createOrFindMember(OAuth2UserInfo oAuth2UserInfo) {
        Optional<Member> memberOptional = memberRepository.findByProviderId(oAuth2UserInfo.getProviderId());

        if (memberOptional.isPresent()) {
            log.info("[회원 조회] 기존 회원 - ID: {}", memberOptional.get().getId());
            return memberOptional.get();
        }

        try {
            Member member = Member.builder()
                    .email(oAuth2UserInfo.getEmail())
                    .nickname(generateRandomNickname())
                    .provider(oAuth2UserInfo.getProvider())
                    .providerId(oAuth2UserInfo.getProviderId())
                    .build();

            Member savedMember = memberRepository.save(member);
            log.info("[회원 생성] 신규 회원 가입 - ID: {}, Provider: {}, 닉네임: {}",
                    savedMember.getId(), savedMember.getProvider(), savedMember.getNickname());

            return savedMember;
        } catch (Exception e) {
            log.error("[회원 생성] 실패 - Provider: {}, 오류: {}", oAuth2UserInfo.getProvider(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Refresh Token을 통해 Access Token을 재발급합니다.
     *
     * @param tokenRequest refresh Token 포함
     * @return tokenResponse access Token 포함
     */
    public TokenResponse createAccessToken(TokenRequest tokenRequest) {
        String refreshToken = tokenRequest.getRefreshToken();

        try {
            if (!jwtProvider.isTokenValidate(refreshToken)) {
                log.error("[액세스 토큰 재발급] 유효하지 않은 리프래시 토큰");
                throw new BusinessException(INVALID_REFRESH_TOKEN);
            }

            Member member = memberRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> {
                        log.error("[액세스 토큰 재발급] 리프래시 토큰을 찾을 수 없음");
                        return new BusinessException(REFRESH_TOKEN_NOT_FOUND);
                    });

            if (!Objects.equals(member.getRefreshToken(), refreshToken)) {
                log.error("[액세스 토큰 재발급] 저장된 리프래시 토큰과 불일치 - 사용자 ID: {}", member.getId());
                throw new BusinessException(INVALID_REFRESH_TOKEN);
            }

            String accessToken = jwtProvider.createAccessToken(member.getId());
            log.info("[액세스 토큰 재발급] 성공 - 사용자 ID: {}", member.getId());

            return new TokenResponse(accessToken);
        } catch (Exception e) {
            log.error("[액세스 토큰 재발급] 실패 - 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 형용사 + 동물 + 숫자 조합으로 10자리 이내의 랜덤한 닉네임을 생성합니다.
     * 50번의 시도에도 중복된 닉네임이 있을 경우, "작은쥐" + 7자리 숫자 조합으로 생성되게 합니다.
     *
     * @return 생성된 닉네임
     */
    private String generateRandomNickname() {
        List<String> adjectives = Arrays.asList(
                "밝은", "좋은", "큰", "작은", "빠른", "느린", "높은", "깊은", "새로운", "오래된",
                "행복한", "귀여운", "따뜻한", "용감한", "똑똑한", "친절한", "상냥한", "소중한",
                "특별한", "건강한", "활발한", "신나는", "웃는", "착한", "예쁜", "멋진",
                "사랑스런", "아름다운", "반짝이는", "포근한"
        );

        List<String> animals = Arrays.asList(
                "곰", "말", "양", "개", "새", "벌",
                "토끼", "여우", "사자", "호랑이", "펭귄", "판다", "코알라",
                "고양이", "강아지", "햄스터", "다람쥐", "원숭이", "돌고래",
                "거북이", "개구리", "나비", "물고기"
        );

        Random random = new Random();
        String nickname = "";
        int attempts = 0;
        int maxAttempts = 50;

        do {
            String adjective = adjectives.get(random.nextInt(adjectives.size()));
            String animal = animals.get(random.nextInt(animals.size()));

            int usedLength = adjective.length() + animal.length();
            int remainLength = 10 - usedLength;

            if (remainLength > 0) {
                // 이미 선택된 형용사, 동물의 자리수를 확인하여, 남은 수를 숫자에 사용 (최소 1자리, 최대 remainLength)
                int randomNum = random.nextInt(remainLength) + 1;
                StringBuilder stringNum = new StringBuilder();
                for (int i = 0; i < randomNum; i++) {
                    stringNum.append(random.nextInt(10));
                }
                nickname = adjective + animal + stringNum.toString();
            }
            attempts++;
        } while (memberRepository.existsByNickname(nickname) && attempts < maxAttempts);

        // 50번 시도 실패 시 "작은쥐" + 현재 시간 기반 7자리 숫자 조합으로 저장
        if (attempts >= maxAttempts) {
            long timestamp = System.currentTimeMillis() % 10000000;
            nickname = "작은쥐" + String.format("%07d", (int)timestamp);
            log.warn("[닉네임 생성] 50번 시도 후 '작은쥐' 조합으로 생성");
        }

        return nickname;
    }
}