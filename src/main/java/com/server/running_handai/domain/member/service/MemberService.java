package com.server.running_handai.domain.member.service;

import com.server.running_handai.domain.bookmark.dto.MyBookmarkDetailDto;
import com.server.running_handai.domain.bookmark.dto.MyBookmarkInfoDto;
import com.server.running_handai.domain.bookmark.service.BookmarkService;
import com.server.running_handai.domain.course.dto.CourseInfoDto;
import com.server.running_handai.domain.course.dto.MyAllCoursesDetailDto;
import com.server.running_handai.domain.course.service.CourseService;
import com.server.running_handai.domain.member.dto.MemberInfoDto;
import com.server.running_handai.domain.member.dto.MemberUpdateRequestDto;
import com.server.running_handai.domain.member.dto.MemberUpdateResponseDto;
import com.server.running_handai.global.entity.SortBy;
import com.server.running_handai.global.jwt.JwtProvider;
import com.server.running_handai.global.oauth.userInfo.OAuth2UserInfo;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.domain.member.dto.TokenRequestDto;
import com.server.running_handai.domain.member.dto.TokenResponseDto;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.entity.Role;
import com.server.running_handai.domain.member.repository.MemberRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    public static final int NICKNAME_MAX_LENGTH = 10;
    public static final int NICKNAME_MIN_LENGTH = 2;
    private static final String NICKNAME_PATTERN = "^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9]{2,10}$";
    private static final int BOOKMARK_PREVIEW_MAX_COUNT = 5;
    private static final int MY_COURSE_PREVIEW_MAX_COUNT = 3;

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final BookmarkService bookmarkService;
    private final CourseService courseService;

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
                    .role(Role.USER)
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
     * Refresh Token Rotation(RTR) 방식을 사용하여 사용된 Refresh Token 역시 재발급합니다.
     *
     * @param tokenRequestDto 인증에 사용될 Refresh Token 포함
     * @return tokenResponseDto 재발급된 Access Token, Refresh Token 포함
     */
    @Transactional
    public TokenResponseDto createToken(TokenRequestDto tokenRequestDto) {
        String refreshToken = tokenRequestDto.refreshToken();

        try {
            if (!jwtProvider.isTokenValidate(refreshToken)) {
                log.error("[액세스 토큰 재발급] 유효하지 않은 리프래시 토큰");
                throw new BusinessException(ResponseCode.INVALID_REFRESH_TOKEN);
            }

            Member member = memberRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> {
                        log.error("[액세스 토큰 재발급] 리프래시 토큰을 찾을 수 없음");
                        return new BusinessException(ResponseCode.REFRESH_TOKEN_NOT_FOUND);
                    });

            // Refresh Token Rotation(RTR) 방식을 적용하여 재발급 시 Access Token, Refresh Token 모두 재발급
            String newAccessToken = jwtProvider.createAccessToken(member.getId());
            String newRefreshToken = jwtProvider.createRefreshToken();
            member.updateRefreshToken(newRefreshToken);
            log.info("[액세스 토큰 재발급] 성공 - 사용자 ID: {}", member.getId());

            return new TokenResponseDto(newAccessToken, newRefreshToken);
        } catch (ExpiredJwtException e) {
            log.error("[액세스 토큰 재발급] 만료된 리프래시 토큰");
            throw new BusinessException(ResponseCode.REFRESH_TOKEN_EXPIRED);
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
            int remainLength = NICKNAME_MAX_LENGTH - usedLength;

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

    /**
     * 닉네임 중복 여부를 조회합니다.
     * 닉네임 유효성 검증도 함께 수행합니다.
     *
     * @param memberId 사용자 Id
     * @param nickname 검증할 닉네임
     * @return 중복이지 않으면 true, 중복이면 false.
     */
    public Boolean checkNicknameDuplicate(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 중복 여부 조회 시 문자 앞, 뒤 공백과 영문 대, 소문자는 무시 (프론트 측에서 처리해서 보내줌)
        String newNickname = nickname.trim().toLowerCase();
        String currentNickname = member.getNickname().trim().toLowerCase();

        return isNicknameValid(newNickname, currentNickname);
    }

    /**
     * 내 정보를 수정합니다.
     * 닉네임 유효성 검증도 함께 수행합니다.
     *
     * @param memberId 사용자 Id
     * @param memberUpdateRequestDto 수정하고 싶은 내 정보 Dto
     * @return 수정된 내 정보 Dto (MemberUpdateResponseDto)
     */
    @Transactional
    public MemberUpdateResponseDto updateMemberInfo(Long memberId, MemberUpdateRequestDto memberUpdateRequestDto) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 중복 여부 조회 시 문자 앞, 뒤 공백과 영문 대, 소문자는 무시 (프론트 측에서 처리해서 보내줌)
        String newNickname = memberUpdateRequestDto.nickname().trim().toLowerCase();
        String currentNickname = member.getNickname().trim().toLowerCase();

        if (isNicknameValid(newNickname, currentNickname)) {
            member.updateNickname(newNickname);
        } else {
            throw new BusinessException(ResponseCode.DUPLICATE_NICKNAME);
        }

        return MemberUpdateResponseDto.from(member.getId(), member.getNickname());
    }

    /**
     * 닉네임 유효성을 검증합니다.
     * 테스트를 위해 가시성을 완화했습니다. (private -> package-private)
     *
     * @param newNickname 검증할 닉네임
     * @param currentNickname 사용자의 현재 닉네임
     * @return 사용 가능하면 true, 사용 불가하면 false.
     */
    boolean isNicknameValid(String newNickname, String currentNickname) {
        // 이미 자신이 사용 중인 닉네임이어서는 안됨
        if (currentNickname.equals(newNickname)) {
            throw new BusinessException(ResponseCode.SAME_AS_CURRENT_NICKNAME);
        }

        // 닉네임 글자수는 2글자부터 최대 10글자까지
        if (newNickname.length() < NICKNAME_MIN_LENGTH || newNickname.length() > NICKNAME_MAX_LENGTH) {
            throw new BusinessException(ResponseCode.INVALID_NICKNAME_LENGTH);
        }

        // 닉네임은 한글, 숫자, 영문만 입력할 수 있음
        if (!newNickname.matches(NICKNAME_PATTERN)) {
            throw new BusinessException(ResponseCode.INVALID_NICKNAME_FORMAT);
        }

        return !memberRepository.existsByNickname(newNickname);
    }

    /**
     * 회원 정보를 조회합니다.
     * 회원의 닉네임, 이메일, 회원이 북마크한 코스, 생성한 코스 정보를 반환합니다.
     *
     * @param memberId 요청 회원의 ID
     * @return 조회한 회원 정보를 담은 DTO
     */
    public MemberInfoDto getMemberInfo(Long memberId) {
        // 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        // 북마크한 코스 조회
        MyBookmarkDetailDto bookmarkedCourses = MyBookmarkDetailDto.from(
                bookmarkService.findBookmarkedCourses(memberId, null).stream()
                        .map(MyBookmarkInfoDto::from)
                        .limit(BOOKMARK_PREVIEW_MAX_COUNT)
                        .toList());

        // 내 코스 조회
        Pageable pageable = PageRequest.of(0, MY_COURSE_PREVIEW_MAX_COUNT, SortBy.findBySort("LATEST"));
        MyAllCoursesDetailDto myCourses = courseService.getMyAllCourses(memberId, pageable, null);

        return MemberInfoDto.from(member, bookmarkedCourses, myCourses);
    }
}