package com.server.running_handai.domain.admin.service;

import com.server.running_handai.domain.admin.dto.AdminLoginRequestDto;
import com.server.running_handai.domain.member.dto.TokenResponseDto;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.repository.MemberRepository;
import com.server.running_handai.domain.member.service.MemberService;
import com.server.running_handai.global.jwt.JwtProvider;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.server.running_handai.global.response.ResponseCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    @Value("${admin.id}")
    private String adminId;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;

    public TokenResponseDto login(AdminLoginRequestDto adminLoginRequestDto) {
        log.info("[관리자 로그인] 시도");
        String email = adminLoginRequestDto.email();
        String password = adminLoginRequestDto.password();

        if (email.equals(adminEmail) && password.equals(adminPassword)) {
            log.info("[관리자 로그인] 성공");
            long id = Long.parseLong(adminId);
            Member admin = memberRepository.findById(id).orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
            String accessToken = jwtProvider.createAccessToken(id);
            String refreshToken = jwtProvider.createRefreshToken(id);
            admin.updateRefreshToken(refreshToken);
            return new TokenResponseDto(accessToken, refreshToken);
        } else {
            throw new BusinessException(UNAUTHORIZED_ACCESS);
        }
    }
}
