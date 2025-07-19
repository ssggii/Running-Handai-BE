package com.server.running_handai.global.oauth;

import com.server.running_handai.global.jwt.JwtProvider;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication)
            throws IOException {
        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = customOAuth2User.getMember();
        log.info("[OAuth2 인증] 성공 - 사용자 ID: {}, JWT 토큰 생성 시작", member.getId());

        String accessToken = jwtProvider.createAccessToken(member.getId());
        String refreshToken = jwtProvider.createRefreshToken();

        member.updateRefreshToken(refreshToken);
        memberRepository.save(member);
        log.info("[JWT 토큰 생성] 완료 - 사용자 ID: {}", member.getId());

        String clientRedirectUrl =
                redirectUrl
                        + "?result=true"
                        + "&accessToken="
                        + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                        + "&refreshToken="
                        + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        httpServletResponse.sendRedirect(clientRedirectUrl);
    }
}