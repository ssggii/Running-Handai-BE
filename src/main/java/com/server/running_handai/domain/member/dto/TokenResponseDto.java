package com.server.running_handai.domain.member.dto;

public record TokenResponseDto (
        String accessToken,
        String refreshToken
) { }