package com.server.running_handai.domain.admin.dto;

public record AdminLoginRequestDto(
        String email,
        String password
) {
}
