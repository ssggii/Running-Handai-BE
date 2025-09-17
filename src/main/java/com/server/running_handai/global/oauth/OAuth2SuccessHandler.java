package com.server.running_handai.global.oauth;

import com.server.running_handai.global.jwt.JwtProvider;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication)
            throws IOException {
        String state = httpServletRequest.getParameter("state");

        // 쉼표(,) 기준으로 공백 제거 후 나누기
        List<String> redirectUriList = Arrays.stream(redirectUris.split(","))
                .map(String::trim)
                .toList();

        String redirectUri = switch (state.toLowerCase()) {
            case "prod" -> redirectUriList.getLast();
            case "local" -> redirectUriList.getFirst();
            default -> {
                log.warn("[OAuth2 인증] 유효하지 않은 state 값입니다: {}", state);
                throw new BusinessException(ResponseCode.BAD_REQUEST_STATE_PARAMETER);
            }
        };
        log.info("[OAuth2 인증] 리다이렉트 URI 설정: state={}, URI={}", state, redirectUri);

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = customOAuth2User.getMember();
        log.info("[OAuth2 인증] 성공: 사용자 ID={}, JWT 토큰 생성 시작", member.getId());

        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);
        log.info("[JWT 토큰 생성] 완료: 사용자 ID={}", member.getId());

        String clientRedirectUrl =
                redirectUri
                        + "?result=true"
                        + "&accessToken="
                        + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                        + "&refreshToken="
                        + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        httpServletResponse.sendRedirect(clientRedirectUrl);
    }
}