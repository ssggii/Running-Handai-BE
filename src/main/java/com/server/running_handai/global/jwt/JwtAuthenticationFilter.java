package com.server.running_handai.global.jwt;

import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.exception.BusinessException;
import com.server.running_handai.member.entity.Member;
import com.server.running_handai.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

import static com.server.running_handai.global.response.ResponseCode.ACCESS_TOKEN_EXPIRED;
import static com.server.running_handai.global.response.ResponseCode.MEMBER_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = httpServletRequest.getRequestURI();

        try {
            // Header에서 Access Token 가져오기
            String token = jwtProvider.getToken(httpServletRequest);

            // Access Token 검증 및 인증 처리
            if (token != null && jwtProvider.isTokenValidate(token)) {
                if (jwtProvider.isTokenExpired(token)) {
                    log.warn("[JWT 인증] 토큰 만료 - URI: {}", requestURI);
                    throw new BusinessException(ACCESS_TOKEN_EXPIRED);
                }

                String id = jwtProvider.getId(token);
                Long memberId = Long.parseLong(id);
                Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

                // Access Token에서 가져온 사용자 정보로 Authentication 객체 만들어 Security Context에 저장
                CustomOAuth2User CustomOAuth2User = new CustomOAuth2User(member, Map.of());
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(CustomOAuth2User, null, CustomOAuth2User.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            // 다음 Filter로 넘기기 (해당 호출이 없으면 요청이 멈춤)
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception e) {
            log.error("[JWT 인증] 실패 -  URI: {}, 오류: {}", requestURI, e.getMessage(), e);
            throw e;
        }
    }
}